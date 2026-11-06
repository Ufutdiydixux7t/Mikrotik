package com.example.mikrotik.presentation.diagnostics

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.core.root.RootStatus
import com.example.mikrotik.core.root.SystemDiagnosticReport
import com.example.mikrotik.presentation.login.LoginScreen
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.theme.AppIcons

private val DeepGreenHeader = Color(0xFF0F3822)
private val RichGreenButton = Color(0xFF00796B)
private val PageBackground = Color(0xFFE6F0EC)
private val SeamlessCardBg = Color(0xFFFFFFFF)
private val WatermarkLineColor = Color(0xFFE0E0E0)

sealed class SettingsDestination {
    object MainSettings : SettingsDestination()
    object SystemInfo : SettingsDestination()
    object LoginPage : SettingsDestination()
    object AboutUs : SettingsDestination()
    object Privacy : SettingsDestination()
    object Language : SettingsDestination()
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf<SettingsDestination>(SettingsDestination.MainSettings) }
    var isDarkModeEnabled by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        AppTopBar(
            title = when (currentDestination) {
                SettingsDestination.MainSettings -> "الإعدادات"
                SettingsDestination.SystemInfo -> "معلومات النظام"
                SettingsDestination.LoginPage -> "واجهة تسجيل الدخول"
                SettingsDestination.AboutUs -> "نظرة عنا"
                SettingsDestination.Privacy -> "البيانات والخصوصية"
                SettingsDestination.Language -> "اللغة"
            },
            startIcon = AppIcons.SettingsGear
        )

        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            when (currentDestination) {
                SettingsDestination.MainSettings -> {
                    MainSettingsList(
                        isDarkModeEnabled = isDarkModeEnabled,
                        onDarkModeToggle = { isDarkModeEnabled = it },
                        onNavigate = { currentDestination = it }
                    )
                }
                SettingsDestination.SystemInfo -> {
                    SystemInfoDetailScreen(
                        uiState = uiState,
                        onRefresh = { viewModel.runDiagnostics() },
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.LoginPage -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        BackToSettingsButton(
                            onClick = { currentDestination = SettingsDestination.MainSettings }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(650.dp)) {
                            LoginScreen()
                        }
                    }
                }
                SettingsDestination.AboutUs -> {
                    AboutUsDetailScreen(
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.Privacy -> {
                    BlankDetailScreen(
                        title = "البيانات والخصوصية",
                        message = "جميع بيانات الحزم والبطاقات مخزنة محلياً في قاعدة البيانات المشفرة.",
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.Language -> {
                    BlankDetailScreen(
                        title = "اللغة",
                        message = "اللغة الحالية: العربية (اللغة الافتراضية)",
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
            }
        }
    }
}

@Composable
fun MainSettingsList(
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    onNavigate: (SettingsDestination) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Language
            SettingsItemRow(
                icon = AppIcons.Globe,
                title = "اللغة",
                subtitle = "العربية",
                onClick = { onNavigate(SettingsDestination.Language) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // Dark Mode Switch Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = AppIcons.Moon,
                        contentDescription = "Dark Mode",
                        tint = RichGreenButton,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "الوضع الداكن",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenHeader
                    )
                }
                Switch(
                    checked = isDarkModeEnabled,
                    onCheckedChange = onDarkModeToggle,
                    colors = SwitchDefaults.colors(checkedThumbColor = RichGreenButton)
                )
            }
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // Login Page
            SettingsItemRow(
                icon = AppIcons.Login,
                title = "واجهة تسجيل الدخول",
                onClick = { onNavigate(SettingsDestination.LoginPage) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // About Us
            SettingsItemRow(
                icon = AppIcons.Info,
                title = "نظرة عنا",
                onClick = { onNavigate(SettingsDestination.AboutUs) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // Privacy
            SettingsItemRow(
                icon = AppIcons.Shield,
                title = "البيانات والخصوصية",
                onClick = { onNavigate(SettingsDestination.Privacy) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // System Info
            SettingsItemRow(
                icon = AppIcons.SystemMonitor,
                title = "معلومات النظام",
                onClick = { onNavigate(SettingsDestination.SystemInfo) }
            )
        }
    }
}

@Composable
private fun BackToSettingsButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = DeepGreenHeader)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("رجوع إلى الإعدادات")
            Icon(
                imageVector = AppIcons.ChevronRight,
                contentDescription = null,
                tint = DeepGreenHeader,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun SettingsItemRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RichGreenButton,
                modifier = Modifier.size(24.dp)
            )
            Column {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenHeader
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        Icon(
            imageVector = AppIcons.ChevronLeft,
            contentDescription = null,
            tint = Color.Gray,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SystemInfoDetailScreen(
    uiState: DiagnosticsUiState,
    onRefresh: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackToSettingsButton(onClick = onBack)

        Spacer(modifier = Modifier.height(4.dp))

        when (uiState) {
            is DiagnosticsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = RichGreenButton)
                }
            }
            is DiagnosticsUiState.Error -> {
                Text(text = "خطأ: ${uiState.message}", color = Color.Red)
            }
            is DiagnosticsUiState.Success -> {
                DiagnosticsReportContent(
                    report = uiState.report,
                    dbStatus = uiState.databaseStatus,
                    onRefresh = onRefresh
                )
            }
        }
    }
}

@Composable
fun AboutUsDetailScreen(
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackToSettingsButton(onClick = onBack)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppIcons.Info,
                    contentDescription = null,
                    tint = RichGreenButton,
                    modifier = Modifier.size(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Mini MikroTik",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "نظام إدارة الشبكات والروتر المصغر",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "نظرة عن المشروع",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "نظام متكامل لإدارة شبكات الهوتسبوت (Hotspot) وتوليد وإنشاء كروت وبطاقات الإنترنت، وتتبع الجلسات النشطة واستهلاك البيانات للمشتركين، بالإضافة إلى التحكم بإعدادات التوجيه والجدار الناري عبر صلاحيات الـ Root.",
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = PageBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "تم تطوير هذا النظام بواسطة م/محمد المدي",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenHeader,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BlankDetailScreen(
    title: String,
    message: String,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BackToSettingsButton(onClick = onBack)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun DiagnosticsReportContent(
    report: SystemDiagnosticReport,
    dbStatus: String,
    onRefresh: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val (cardColor, textColor, statusTitle, statusSubtitle) = when (report.rootStatus) {
            is RootStatus.Granted -> Quadruple(
                Color(0xFFE8F5E9),
                Color(0xFF2E7D32),
                "Root Granted (uid=0)",
                report.userIdOutput
            )
            is RootStatus.Denied -> Quadruple(
                Color(0xFFFFEBEE),
                Color(0xFFC62828),
                "Root DENIED",
                "تم رفض طلب صلاحيات الـ Root من Magisk أو تم إلغاؤه."
            )
            is RootStatus.NotAvailable -> Quadruple(
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                "Root NOT AVAILABLE",
                report.rootStatusMessage
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "حالة صلاحيات Root:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusTitle,
                    color = textColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = statusSubtitle,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray
                )
            }
        }

        Text(
            text = "تفاصيل البيئة والنظام:",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DeepGreenHeader
        )

        InfoItem("قاعدة البيانات المحلية (Room)", dbStatus)
        InfoItem("نتيجة أمر التحقق 'id'", report.userIdOutput)
        InfoItem("النواة (Kernel)", report.kernelInfo)
        InfoItem("وضع SELinux", report.selinuxMode)
        InfoItem("نسخة iptables", report.iptablesVersion)
        InfoItem("نسخة iptables-legacy", report.iptablesLegacyVersion)
        InfoItem("نسخة iptables-nft", report.iptablesNftVersion)
        InfoItem("نسخة nftables", report.nftablesVersion)
        InfoItem("توجيه الحزم (IP Forwarding)", report.ipForwardingEnabled)
        InfoItem("أمر Tethering الداخلي", report.tetheringCmdResponse)

        Spacer(modifier = Modifier.height(8.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "واجهات الشبكة المتاحة (Interfaces):",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DeepGreenHeader
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (report.availableNetworkInterfaces.isEmpty()) {
                    Text("لم يتم كشف واجهات (يتطلب Root ممنوح).", fontSize = 13.sp)
                } else {
                    report.availableNetworkInterfaces.forEach { iface ->
                        Text("• $iface", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = DeepGreenHeader)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "سجل الأوامر التشخيصية (Raw Logs):",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DeepGreenHeader
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(8.dp))
                .padding(12.dp)
        ) {
            Text(
                text = report.rawLog.joinToString("\n"),
                color = Color(0xFF00FF66),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("طلب صلاحيات Root وإعادة الفحص")
        }
    }
}

private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

@Composable
fun InfoItem(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(10.dp)),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, fontSize = 13.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenHeader)
        }
    }
}
