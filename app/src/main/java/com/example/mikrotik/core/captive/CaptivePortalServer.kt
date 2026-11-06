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
                        call.respondText(getHtmlLoginPage(), contentType = ContentType.Text.Html)
                    }

                    // --- Captive Portal OS Detection Probes ---
                    get("/generate_204") {
                        call.respondRedirect("http://${getActiveHotspotIpAddress()}:$port/")
                    }
                    get("/gen_204") {
                        call.respondRedirect("http://${getActiveHotspotIpAddress()}:$port/")
                    }
                    get("/hotspot-detect.html") {
                        call.respondText(getHtmlLoginPage(), contentType = ContentType.Text.Html)
                    }
                    get("/library/test/success.html") {
                        call.respondText(getHtmlLoginPage(), contentType = ContentType.Text.Html)
                    }
                    get("/ncsi.txt") {
                        call.respondText("Microsoft NCSI", contentType = ContentType.Text.Plain)
                    }
                    get("/connecttest.txt") {
                        call.respondText("Microsoft Connect Test", contentType = ContentType.Text.Plain)
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
                            val clientIp = extractIpv4(call.request.local.remoteAddress)
                            val client = clientIp?.let { ip ->
                                clientDiscoveryManager.getConnectedDevices().firstOrNull { it.ipAddress == ip }
                            }
                            if (client == null) {
                                call.respondText(
                                    getHtmlResultPage("تعذر تحديد الجهاز", "أعد الاتصال بشبكة Hotspot ثم حاول مرة أخرى.", false),
                                    contentType = ContentType.Text.Html
                                )
                                return@post
                            }

                            val activated = voucherDao.activateVoucherIfValid(code, client.macAddress, now)
                            val voucher = voucherDao.getVoucherByCode(code)

                            if (activated > 0 && voucher != null) {
                                val isAllowed = iptablesManager.authorizeMacAddress(client.macAddress)
                                if (!isAllowed) {
                                    call.respondText(
                                        getHtmlResultPage("تعذر فتح الاتصال", "تم التحقق من البطاقة، لكن تعذر تطبيق قاعدة السماح على الهاتف. أعد المحاولة أو راجع التشخيص.", false),
                                        contentType = ContentType.Text.Html
                                    )
                                    return@post
                                }
                                activeSessionDao.insertOrUpdateSession(
                                    ActiveSessionEntity(
                                        macAddress = client.macAddress,
                                        ipAddress = client.ipAddress,
                                        voucherCode = code
                                    )
                                )
                                val remainingBytes = (voucher.dataLimitBytes - voucher.bytesUsedSoFar).coerceAtLeast(0L)
                                val remainingMb = remainingBytes / (1024L * 1024L)
                                val firstUse = voucher.firstUsedAt ?: now
                                val remainingMinutes = ((firstUse + voucher.durationMinutes * 60_000L - now) / 60_000L)
                                    .coerceAtLeast(0L)
                                val message = "تم قبول البطاقة والسماح لهذا الجهاز. المتبقي: $remainingMb MB و$remainingMinutes دقيقة."
                                call.respondText(
                                    getHtmlResultPage("تم قبول البطاقة", message, true),
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

    private fun extractIpv4(remoteAddress: String): String? {
        return Regex("(?:^|/)(\\d{1,3}(?:\\.\\d{1,3}){3})(?::\\d+)?$")
            .find(remoteAddress.trim())
            ?.groupValues
            ?.getOrNull(1)
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
                .header {
                    margin-top: 40px;
                    margin-bottom: 30px;
                }
                .logo-icon {
                    width: 64px;
                    height: 64px;
                    fill: #00796B;
                    margin-bottom: 8px;
                }
                .brand-title {
                    font-size: 24px;
                    font-weight: bold;
                    color: #0F3822;
                    letter-spacing: 1px;
                }
                .login-card {
                    background: #FFFFFF;
                    padding: 32px 24px;
                    border-radius: 20px;
                    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
                }
                .login-title {
                    font-size: 20px;
                    font-weight: bold;
                    color: #0F3822;
                    margin-bottom: 8px;
                }
                .login-subtitle {
                    font-size: 14px;
                    color: #666666;
                    margin-bottom: 24px;
                }
                .input-group {
                    margin-bottom: 20px;
                }
                .input-field {
                    width: 100%;
                    padding: 14px 16px;
                    font-size: 16px;
                    border: 1.5px solid #C5E0D6;
                    border-radius: 12px;
                    box-sizing: border-box;
                    outline: none;
                    text-align: center;
                    background: #FAFCFB;
                }
                .input-field:focus {
                    border-color: #00796B;
                }
                .btn-submit {
                    width: 100%;
                    padding: 14px;
                    background-color: #00796B;
                    color: white;
                    border: none;
                    border-radius: 12px;
                    font-size: 18px;
                    font-weight: bold;
                    cursor: pointer;
                    box-shadow: 0 4px 10px rgba(0,121,107,0.3);
                }
                .btn-submit:active {
                    background-color: #005B4F;
                }
                .footer {
                    margin-bottom: 20px;
                    font-size: 12px;
                    color: #888888;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <svg class="logo-icon" viewBox="0 0 24 24">
                        <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" fill="#00796B"/>
                    </svg>
                    <div class="brand-title">AL-MADDI NET</div>
                </div>

                <div class="login-card">
                    <div class="login-title">تسجيل الدخول</div>
                    <div class="login-subtitle">أدخل البطاقة للاتصال بالإنترنت</div>
                    
                    <form action="/login" method="POST">
                        <div class="input-group">
                            <input type="text" name="code" class="input-field" placeholder="أدخل الكرت هنا" required autocomplete="off">
                        </div>
                        <button type="submit" class="btn-submit">اتصال</button>
                    </form>
                </div>
            </div>

            <div class="footer">
                Powered by Hotspot
            </div>
        </body>
        </html>
        """.trimIndent()
    }

    private fun getHtmlResultPage(title: String, message: String, isSuccess: Boolean): String {
        val color = if (isSuccess) "#00796B" else "#C62828"
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
                .message {
                    font-size: 16px;
                    color: #333333;
                    margin-bottom: 24px;
                    line-height: 1.6;
                }
                .btn-back {
                    display: inline-block;
                    padding: 12px 24px;
                    background-color: #00796B;
                    color: white;
                    text-decoration: none;
                    border-radius: 12px;
                    font-size: 16px;
                    font-weight: bold;
                }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="card">
                    <div class="title">$title</div>
                    <div class="message">$message</div>
                    <a href="/" class="btn-back">العودة</a>
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
