package com.example.mikrotik.core.captive

import android.util.Log
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.cio.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import com.example.mikrotik.core.hotspot.ClientDiscoveryManager
import com.example.mikrotik.core.network.IptablesManager
import com.example.mikrotik.data.db.dao.ActiveSessionDao
import com.example.mikrotik.data.db.dao.BlockedMacDao
import com.example.mikrotik.data.db.dao.VoucherDao
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import java.net.NetworkInterface
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CaptivePortalServer @Inject constructor(
    private val voucherDao: VoucherDao,
    private val activeSessionDao: ActiveSessionDao,
    private val blockedMacDao: BlockedMacDao,
    private val clientDiscoveryManager: ClientDiscoveryManager,
    private val iptablesManager: IptablesManager
) {

    private var server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>? = null
    private var isRunning = false
    private val port = 8080

    @Synchronized
    fun startServer(): String {
        if (isRunning && server != null) {
            Log.d("CaptivePortalServer", "Server is already running at ${getLocalAccessUrl()}")
            return getLocalAccessUrl()
        }

        try {
            val ip = getActiveHotspotIpAddress()
            Log.d("CaptivePortalServer", "Starting Ktor server on host 0.0.0.0, port $port, Hotspot IP: $ip")

            server = embeddedServer(CIO, host = "0.0.0.0", port = port) {
                routing {
                    intercept(ApplicationCallPipeline.Call) {
                        val clientIp = call.request.local.remoteAddress
                        val uri = call.request.uri
                        Log.d("CaptivePortalServer", "Incoming request: IP=$clientIp, URI=$uri")
                    }

                    // 4. Health Check Route
                    get("/health") {
                        val statusJson = "{\"status\": \"running\", \"ip\": \"${getActiveHotspotIpAddress()}\", \"port\": $port}"
                        call.respondText(statusJson, contentType = ContentType.Application.Json)
                    }

                    // Main login page
                    get("/") {
                        call.respondCaptiveLoginPage()
                    }

                    // --- Captive Portal OS Detection Probes ---
                    get("/generate_204") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/gen_204") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/hotspot-detect.html") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/library/test/success.html") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/ncsi.txt") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/connecttest.txt") {
                        call.respondCaptiveLoginPage()
                    }
                    get("/redirect") {
                        call.respondCaptiveLoginPage()
                    }

                    // Login submission
                    post("/login") {
                        val params = call.receiveParameters()
                        val code = params["code"].orEmpty().trim().uppercase(Locale.ROOT)
                        Log.d("CaptivePortalServer", "Voucher login attempt submitted")

                        if (code.isBlank()) {
                            call.respondText(getHtmlResultPage("خطأ", "يرجى إدخال كود الكرت بشكل صحيح.", false), contentType = ContentType.Text.Html)
                        } else {
                            val now = System.currentTimeMillis()
                            val remoteAddress = call.request.local.remoteAddress
                            val clientIp = extractIpv4(call.request.local.remoteHost)
                                ?: extractIpv4(remoteAddress)
                            val client = clientIp?.let { ip ->
                                val mac = clientDiscoveryManager.findMacAddressForIp(ip)
                                val discovered = if (mac == null) {
                                    clientDiscoveryManager.getConnectedDevices().firstOrNull { it.ipAddress == ip }
                                } else null
                                val resolvedMac = mac ?: discovered?.macAddress
                                resolvedMac?.let {
                                    discovered ?: com.example.mikrotik.domain.model.ConnectedDevice(
                                        ipAddress = ip,
                                        macAddress = it,
                                        connectionState = "CONNECTED",
                                        interfaceName = "hotspot"
                                    )
                                }
                            }
                            if (client == null) {
                                Log.w(
                                    "CaptivePortalServer",
                                    "Could not resolve hotspot client. remoteHost=${call.request.local.remoteHost}, remoteAddress=$remoteAddress, parsedIp=$clientIp"
                                )
                                call.respondText(
                                    getHtmlResultPage("تعذر تحديد الجهاز", "أعد الاتصال بشبكة Hotspot ثم حاول مرة أخرى.", false),
                                    contentType = ContentType.Text.Html
                                )
                                return@post
                            }

                            if (blockedMacDao.isMacBlocked(client.macAddress)) {
                                call.respondText(
                                    getHtmlResultPage("الجهاز محظور", "هذا الجهاز محظور من لوحة التحكم. أزل الحظر أولاً ثم أعد المحاولة.", false),
                                    contentType = ContentType.Text.Html
                                )
                                return@post
                            }

                            val activated = voucherDao.activateVoucherIfValid(code, client.macAddress, now)
                            val voucher = voucherDao.getVoucherByCode(code)

                            if (activated > 0 && voucher != null) {
                                if (!iptablesManager.unblockMacAddress(client.macAddress)) {
                                    Log.w("CaptivePortalServer", "Could not clear a stale temporary block before voucher login")
                                }
                                if (!iptablesManager.resetDeviceUsageCounters(client.macAddress, client.ipAddress)) {
                                    Log.w("CaptivePortalServer", "Could not reset traffic counters for a new voucher")
                                }
                                activeSessionDao.insertOrUpdateSession(
                                    ActiveSessionEntity(
                                        macAddress = client.macAddress,
                                        ipAddress = client.ipAddress,
                                        voucherCode = code
                                    )
                                )
                                val isAllowed = iptablesManager.authorizeMacAddress(client.macAddress)
                                if (!isAllowed) {
                                    activeSessionDao.deactivateSession(client.macAddress)
                                    call.respondText(
                                        getHtmlResultPage("تعذر فتح الاتصال", "تم التحقق من البطاقة، لكن تعذر تطبيق قاعدة السماح على الهاتف. أعد المحاولة أو راجع التشخيص.", false),
                                        contentType = ContentType.Text.Html
                                    )
                                    return@post
                                }
                                val currentVoucher = voucherDao.getVoucherByCode(code)
                                if (currentVoucher == null || currentVoucher.isBlocked) {
                                    iptablesManager.revokeMacAddress(client.macAddress)
                                    activeSessionDao.deactivateSession(client.macAddress)
                                    activeSessionDao.deleteSession(client.macAddress)
                                    call.respondText(
                                        getHtmlResultPage("البطاقة لم تعد متاحة", "تم حذف البطاقة أو إيقافها. أدخل بطاقة أخرى للاتصال.", false),
                                        contentType = ContentType.Text.Html
                                    )
                                    return@post
                                }
                                call.respondText(
                                    getHtmlResultPage(
                                        "تم تسجيل الدخول بنجاح",
                                        "تم تفعيل البطاقة والاتصال بالإنترنت بنجاح.",
                                        true
                                    ),
                                    contentType = ContentType.Text.Html
                                )
                            } else {
                                val reason = when {
                                    voucher == null -> "رمز البطاقة غير موجود."
                                    voucher.isBlocked -> "هذه البطاقة محظورة."
                                    voucher.boundMacAddress != null && voucher.boundMacAddress != client.macAddress -> "هذه البطاقة مستخدمة على جهاز آخر."
                                    voucher.bytesUsedSoFar >= voucher.dataLimitBytes -> "انتهى حجم البيانات المسموح لهذه البطاقة."
                                    voucher.firstUsedAt != null && now >= voucher.firstUsedAt + voucher.durationMinutes * 60_000L ->
                                        "انتهت مدة صلاحية البطاقة."
                                    else -> "تعذر قبول البطاقة؛ تحقق من حالتها وحاول مجدداً."
                                }
                                call.respondText(
                                    getHtmlResultPage("تعذر تسجيل الدخول", reason, false),
                                    contentType = ContentType.Text.Html
                                )
                            }
                        }
                    }
                }
            }.start(wait = false)

            isRunning = true
            Log.d("CaptivePortalServer", "Ktor Server successfully started and verified.")
        } catch (e: Exception) {
            isRunning = false
            server = null
            Log.e("CaptivePortalServer", "Failed to start Ktor server", e)
        }

        return getLocalAccessUrl()
    }

    private suspend fun ApplicationCall.respondCaptiveLoginPage() {
        response.headers.append(HttpHeaders.CacheControl, "no-store, no-cache, must-revalidate")
        response.headers.append(HttpHeaders.Pragma, "no-cache")
        respondText(getHtmlLoginPage(), contentType = ContentType.Text.Html)
    }

    private fun extractIpv4(remoteAddress: String): String? {
        val pattern = Regex("(?<![0-9])(\\d{1,3}(?:\\.\\d{1,3}){3})(?![0-9])")
        return pattern.findAll(remoteAddress)
            .map { it.groupValues[1] }
            .firstOrNull { address -> address.split('.').all { it.toIntOrNull()?.let { octet -> octet in 0..255 } == true } }
    }

    @Synchronized
    fun stopServer() {
        try {
            server?.stop(500, 1000)
            server = null
            isRunning = false
            Log.d("CaptivePortalServer", "Ktor Server stopped successfully.")
        } catch (e: Exception) {
            Log.e("CaptivePortalServer", "Error stopping Ktor server", e)
        }
    }

    fun isServerRunning(): Boolean = isRunning && server != null

    fun getPort(): Int = port

    fun getLocalAccessUrl(): String {
        val ip = getActiveHotspotIpAddress()
        return "http://$ip:$port"
    }

    /**
     * 1 & 2. Discovers the active Hotspot interface IPv4 address precisely,
     * checking common AP interfaces (ap0, swlan0, softap0) and hotspot subnets first.
     */
    fun getActiveHotspotIpAddress(): String {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                val name = iface.name.lowercase()
                // Check if interface is a known AP / hotspot interface
                if (name.contains("ap") || name.contains("swlan") || name.contains("softap") || name.contains("bridge")) {
                    val addresses = iface.inetAddresses
                    while (addresses.hasMoreElements()) {
                        val addr = addresses.nextElement()
                        if (!addr.isLoopbackAddress && addr.hostAddress?.indexOf(':') == -1) {
                            val host = addr.hostAddress
                            if (host != null) {
                                Log.d("CaptivePortalServer", "Found Hotspot interface [$name] IP: $host")
                                return host
                            }
                        }
                    }
                }
            }

            // Fallback: search for standard hotspot subnets (192.168.43.x, 192.168.49.x, 192.168.50.x)
            val allInterfaces = NetworkInterface.getNetworkInterfaces()
            while (allInterfaces.hasMoreElements()) {
                val iface = allInterfaces.nextElement()
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress && addr.hostAddress?.indexOf(':') == -1) {
                        val host = addr.hostAddress
                        if (host != null && (host.startsWith("192.168.43.") || host.startsWith("192.168.49.") || host.startsWith("192.168.50."))) {
                            Log.d("CaptivePortalServer", "Found subnet IP: $host on [${iface.name}]")
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CaptivePortalServer", "Error getting hotspot IP", e)
        }
        return "192.168.43.1"
    }

    private fun getHtmlLoginPage(): String {
        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>تسجيل الدخول - AL-MADDI NET</title>
            <style>
                * { box-sizing: border-box; }
                body {
                    min-height: 100vh;
                    min-height: 100svh;
                    margin: 0;
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    overflow-x: hidden;
                    background: #F8FAF9;
                    color: #102D46;
                    font-family: Tahoma, Arial, sans-serif;
                }
                .top-shape {
                    position: fixed;
                    inset: 0 auto auto 0;
                    width: 100%;
                    height: 190px;
                    background: #DDF1ED;
                    clip-path: polygon(0 0, 72% 0, 0 100%);
                    pointer-events: none;
                    z-index: 0;
                }
                .portal-content {
                    position: relative;
                    z-index: 1;
                    display: flex;
                    flex: 1;
                    flex-direction: column;
                    align-items: center;
                    width: 100%;
                    max-width: 560px;
                    padding: clamp(74px, 11vh, 145px) 28px 24px;
                }
                .brand-header {
                    display: flex;
                    flex-direction: column;
                    align-items: center;
                    margin-bottom: clamp(46px, 8vh, 104px);
                    text-align: center;
                }
                .logo-icon {
                    width: 112px;
                    height: 86px;
                    margin-bottom: 10px;
                }
                .brand-title {
                    color: #102D46;
                    font-size: clamp(25px, 6vw, 34px);
                    font-weight: 800;
                    letter-spacing: 1px;
                }
                .login-card {
                    width: 100%;
                    max-width: 500px;
                    padding: 0;
                    text-align: center;
                }
                .portal-mark {
                    width: 58px;
                    height: 50px;
                    margin-bottom: 12px;
                }
                .login-title {
                    margin-bottom: 8px;
                    color: #102D46;
                    font-size: clamp(23px, 5vw, 29px);
                    font-weight: 800;
                }
                .login-subtitle {
                    margin-bottom: 24px;
                    color: #64717D;
                    font-size: 15px;
                    line-height: 1.7;
                }
                .input-group {
                    position: relative;
                    margin: 0 0 22px;
                }
                .input-icon {
                    position: absolute;
                    top: 50%;
                    right: 18px;
                    width: 24px;
                    height: 24px;
                    transform: translateY(-50%);
                    color: #64717D;
                    pointer-events: none;
                }
                .input-field {
                    width: 100%;
                    min-height: 64px;
                    padding: 16px 56px 16px 18px;
                    border: 1.5px solid #D3D9DC;
                    border-radius: 16px;
                    outline: none;
                    background: rgba(255,255,255,0.82);
                    color: #102D46;
                    direction: rtl;
                    text-align: right;
                    font: inherit;
                    font-size: 17px;
                }
                .input-field::placeholder { color: #71808B; }
                .input-field:focus {
                    border-color: #138D86;
                    box-shadow: 0 0 0 3px rgba(19,141,134,0.12);
                }
                .btn-submit {
                    width: 100%;
                    min-height: 64px;
                    border: 0;
                    border-radius: 16px;
                    background: #138D86;
                    color: #FFFFFF;
                    font-family: inherit;
                    font-size: 20px;
                    font-weight: 700;
                    cursor: pointer;
                    box-shadow: 0 8px 18px rgba(19,141,134,0.18);
                }
                .btn-submit:active { background: #0E756F; }
                .footer {
                    position: relative;
                    z-index: 1;
                    display: flex;
                    align-items: center;
                    justify-content: center;
                    gap: 14px;
                    width: min(82%, 430px);
                    margin: auto auto 0;
                    padding-bottom: clamp(138px, 18vh, 220px);
                    color: #596773;
                    font-size: 14px;
                    white-space: nowrap;
                }
                .footer-line {
                    flex: 1;
                    height: 1px;
                    background: #CBD4D5;
                }
                .footer strong { color: #0F817B; }
                .waves {
                    position: fixed;
                    right: 0;
                    bottom: 0;
                    left: 0;
                    z-index: 0;
                    width: 100%;
                    height: clamp(150px, 22vh, 245px);
                    pointer-events: none;
                }
                @media (max-height: 700px) {
                    .portal-content { padding-top: 42px; }
                    .brand-header { margin-bottom: 28px; }
                    .footer { padding-bottom: 125px; }
                    .waves { height: 135px; }
                }
            </style>
        </head>
        <body>
            <div class="top-shape" aria-hidden="true"></div>
            <main class="portal-content">
                <header class="brand-header">
                    <svg class="logo-icon" viewBox="0 0 160 108" role="img" aria-label="AL-MADDI NET">
                        <path d="M20 29C53 1 107 1 140 29" fill="none" stroke="#102D46" stroke-width="11" stroke-linecap="round"/>
                        <path d="M39 49C62 30 98 30 121 49" fill="none" stroke="#138D86" stroke-width="10" stroke-linecap="round"/>
                        <path d="M59 69C71 59 89 59 101 69" fill="none" stroke="#138D86" stroke-width="9" stroke-linecap="round"/>
                        <circle cx="80" cy="83" r="7" fill="#102D46"/>
                        <path d="M14 94C51 103 104 98 145 52" fill="none" stroke="#138D86" stroke-width="7" stroke-linecap="round"/>
                    </svg>
                    <div class="brand-title">AL-MADDI NET</div>
                </header>

                <section class="login-card">
                    <svg class="portal-mark" viewBox="0 0 64 56" aria-hidden="true">
                        <path d="M8 15C21 4 43 4 56 15M16 27C25 19 39 19 48 27M25 39C29 35 35 35 39 39" fill="none" stroke="#138D86" stroke-width="6" stroke-linecap="round"/>
                        <circle cx="32" cy="48" r="4" fill="#138D86"/>
                    </svg>
                    <div class="login-title">تسجيل الدخول</div>
                    <div class="login-subtitle">أدخل البطاقة للاتصال بالإنترنت</div>

                    <form action="/login" method="POST">
                        <div class="input-group">
                            <svg class="input-icon" viewBox="0 0 24 24" fill="none" aria-hidden="true">
                                <rect x="3" y="5" width="18" height="14" rx="2" stroke="currentColor" stroke-width="1.8"/>
                                <path d="M3 9h18M7 15h4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
                            </svg>
                            <input type="text" name="code" class="input-field" placeholder="أدخل الكرت هنا" required autocomplete="off" dir="rtl">
                        </div>
                        <button type="submit" class="btn-submit">اتصال</button>
                    </form>
                </section>
            </main>

            <footer class="footer">
                <span class="footer-line" aria-hidden="true"></span>
                <span>Powered by <strong>Hotspot</strong></span>
                <span class="footer-line" aria-hidden="true"></span>
            </footer>
            <svg class="waves" viewBox="0 0 1200 320" preserveAspectRatio="none" aria-hidden="true">
                <path d="M0 35C185 55 252 165 430 196C616 229 704 168 876 101C1024 44 1102 54 1200 38V320H0Z" fill="#E7F5F2"/>
                <path d="M0 142C186 191 308 161 474 131C674 94 819 209 1002 185C1096 172 1147 143 1200 132V320H0Z" fill="#B9E3DA" opacity="0.92"/>
                <path d="M0 207C196 199 278 222 452 265C637 311 796 299 976 254C1068 231 1131 199 1200 181V320H0Z" fill="#168F87" opacity="0.92"/>
                <path d="M0 250C181 204 288 208 444 240C606 274 704 302 843 278C1002 250 1082 183 1200 159" fill="none" stroke="#7ACCC2" stroke-width="2" opacity="0.75"/>
            </svg>
        </body>
        </html>
        """.trimIndent()
    }

    private fun getHtmlResultPage(title: String, message: String, isSuccess: Boolean): String {
        val color = if (isSuccess) "#00796B" else "#C62828"
        val continueUrl = if (isSuccess) "http://neverssl.com/" else "/"
        val continueLabel = if (isSuccess) "انتقال إلى الرئيسية" else "العودة"
        val successIcon = if (isSuccess) {
            """
            <div class="success-icon" role="img" aria-label="تم بنجاح">
                <svg viewBox="0 0 64 64" aria-hidden="true">
                    <circle cx="32" cy="32" r="29" fill="#E4F4EF" stroke="#138D86" stroke-width="3"/>
                    <path d="M18 33l9 9 19-21" fill="none" stroke="#138D86" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
            </div>
            """.trimIndent()
        } else {
            ""
        }
        return """
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$title - AL-MADDI NET</title>
            <style>
                body {
                    background-color: #E6F0EC;
                    font-family: Tahoma, Arial, sans-serif;
                    margin: 0;
                    padding: 0;
                    display: flex;
                    flex-direction: column;
                    min-height: 100vh;
                    justify-content: space-between;
                    align-items: center;
                }
                .container {
                    width: 100%;
                    max-width: 400px;
                    padding: 24px;
                    box-sizing: border-box;
                    text-align: center;
                }
                .card {
                    background: #FFFFFF;
                    padding: 36px 24px;
                    border-radius: 20px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
                    margin-top: 60px;
                }
                .title {
                    font-size: 22px;
                    font-weight: bold;
                    color: $color;
                    margin-bottom: 16px;
                }
                .success-icon { width: 68px; height: 68px; margin: 0 auto 18px; }
                .success-icon svg { width: 100%; height: 100%; }
                .message {
                    font-size: 16px;
                    color: #333333;
                    margin-bottom: 24px;
                    line-height: 1.6;
                }
                .btn-back {
                    display: inline-block;
                    min-width: 220px;
                    padding: 12px 24px;
                    background-color: #138D86;
                    color: white;
                    text-decoration: none;
                    border-radius: 12px;
                    font-size: 16px;
                    font-weight: bold;
                    box-sizing: border-box;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="card">
                    $successIcon
                    <div class="title">$title</div>
                    <div class="message">$message</div>
                    <a href="$continueUrl" class="btn-back">$continueLabel</a>
                </div>
            </div>
            <div style="margin-bottom: 20px; font-size: 12px; color: #888888;">
                Powered by Hotspot
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
