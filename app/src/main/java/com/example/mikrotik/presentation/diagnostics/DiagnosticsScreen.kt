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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.core.captive.PortalTheme
import com.example.mikrotik.core.root.RootStatus
import com.example.mikrotik.core.root.SystemDiagnosticReport
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.localization.AppLanguage
import com.example.mikrotik.presentation.ui.localization.LocalAppLanguage
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val PageBackground get() = AppPalette.Background
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border

sealed class SettingsDestination {
    object MainSettings : SettingsDestination()
    object SystemInfo : SettingsDestination()
    object Measurement : SettingsDestination()
    object LoginPage : SettingsDestination()
    object AboutUs : SettingsDestination()
    object Privacy : SettingsDestination()
    object Language : SettingsDestination()
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onUsageReset: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf<SettingsDestination>(SettingsDestination.MainSettings) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(
            title = when (currentDestination) {
                SettingsDestination.MainSettings -> appText("الإعدادات", "Settings")
                SettingsDestination.SystemInfo -> appText("معلومات النظام", "System information")
                SettingsDestination.Measurement -> appText("القياس", "Usage measurement")
                SettingsDestination.LoginPage -> appText("واجهة تسجيل الدخول", "Login portal")
                SettingsDestination.AboutUs -> appText("نظرة عنا", "About")
                SettingsDestination.Privacy -> appText("البيانات والخصوصية", "Data & privacy")
                SettingsDestination.Language -> appText("اللغة", "Language")
            },
            subtitle = "AL RABDI NET",
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
                        onDarkModeToggle = onDarkModeToggle,
                        language = language,
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
                SettingsDestination.Measurement -> {
                    com.example.mikrotik.presentation.settings.UsageMeasurementScreen(
                        onBack = { currentDestination = SettingsDestination.MainSettings },
                        onResetComplete = onUsageReset
                    )
                }
                SettingsDestination.LoginPage -> {
                    PortalAppearanceSettingsScreen(
                        viewModel = viewModel,
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.AboutUs -> {
                    AboutUsDetailScreen(
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.Privacy -> {
                    BlankDetailScreen(
                        title = appText("البيانات والخصوصية", "Data & privacy"),
                        message = appText(
                            "جميع بيانات الحزم والبطاقات مخزنة محلياً في قاعدة البيانات المشفرة.",
                            "All voucher and session data is stored locally in the app database."
                        ),
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
                SettingsDestination.Language -> {
                    LanguageSettingsScreen(
                        language = language,
                        onLanguageChange = onLanguageChange,
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageSettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                modifier = Modifier.fillMaxWidth().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = appText("لغة التطبيق", "App language"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Text(
                    text = appText(
                        "اختر لغة الواجهة؛ سيتغير اتجاه المحاذاة تلقائياً.",
                        "Choose the interface language. Text direction and alignment update automatically."
                    ),
                    fontSize = 13.sp,
                    color = AppPalette.Muted
                )
                AppLanguage.entries.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onLanguageChange(option) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = language == option,
                            onClick = { onLanguageChange(option) },
                            colors = RadioButtonDefaults.colors(selectedColor = RichGreenButton)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = option.nativeName,
                            fontSize = 16.sp,
                            fontWeight = if (language == option) FontWeight.Bold else FontWeight.Normal,
                            color = DeepGreenHeader
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalAppearanceSettingsScreen(
    viewModel: DiagnosticsViewModel,
    onBack: () -> Unit
) {
    val appearance by viewModel.portalAppearance.collectAsState()
    val colors = mapOf(
        PortalTheme.GREEN to Color(0xFF138D86),
        PortalTheme.BLUE to Color(0xFF1976D2),
        PortalTheme.RED to Color(0xFFD84343)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = appText("مظهر صفحة الدخول للأجهزة", "Captive portal appearance"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Text(
                    text = appText(
                        "غيّر الاسم واللون اللذين يراهما المتصلون عند فتح بوابة تسجيل الدخول.",
                        "Choose the name and color clients see on the sign-in page."
                    ),
                    fontSize = 13.sp,
                    color = AppPalette.Muted,
                    lineHeight = 20.sp
                )
                OutlinedTextField(
                    value = appearance.name,
                    onValueChange = viewModel::updatePortalName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(appText("اسم واجهة تسجيل الدخول", "Sign-in page name")) },
                    placeholder = { Text("Example: AL-MADDI NET") },
                    singleLine = true,
                    supportingText = {
                        Text(appText(
                            "حتى ${com.example.mikrotik.core.captive.PortalAppearanceSettings.MAX_NAME_LENGTH} حرفاً؛ التغيير يُحفظ تلقائياً",
                            "Up to ${com.example.mikrotik.core.captive.PortalAppearanceSettings.MAX_NAME_LENGTH} characters; saved automatically"
                        ))
                    }
                )
                Text(
                    text = appText("لون الواجهة", "Portal color"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenHeader
                )
                PortalTheme.values().forEach { theme ->
                    val selected = appearance.theme == theme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { viewModel.updatePortalTheme(theme) }
                            .background(if (selected) colors.getValue(theme).copy(alpha = 0.08f) else Color.Transparent)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { viewModel.updatePortalTheme(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = colors.getValue(theme))
                        )
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .background(colors.getValue(theme), RoundedCornerShape(9.dp))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (theme) {
                                PortalTheme.GREEN -> appText("أخضر", "Green")
                                PortalTheme.BLUE -> appText("أزرق", "Blue")
                                PortalTheme.RED -> appText("أحمر", "Red")
                            },
                            color = DeepGreenHeader,
                            fontSize = 14.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettingsList(
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    language: AppLanguage,
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
                title = appText("اللغة", "Language"),
                subtitle = language.nativeName,
                onClick = { onNavigate(SettingsDestination.Language) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            SettingsItemRow(
                icon = AppIcons.BarChart,
                title = appText("القياس", "Usage measurement"),
                subtitle = appText("استهلاك الهاتف ومجموع البطاقات", "Phone and combined voucher usage"),
                onClick = { onNavigate(SettingsDestination.Measurement) }
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
                        contentDescription = appText("الوضع الداكن", "Dark mode"),
                        tint = RichGreenButton,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = appText("الوضع الداكن", "Dark mode"),
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
                title = appText("واجهة تسجيل الدخول", "Login portal"),
                subtitle = appText("تعديل الاسم واللون للأجهزة المتصلة", "Customize name and color for connected clients"),
                onClick = { onNavigate(SettingsDestination.LoginPage) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // About Us
            SettingsItemRow(
                icon = AppIcons.Info,
                title = appText("نظرة عنا", "About"),
                onClick = { onNavigate(SettingsDestination.AboutUs) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // Privacy
            SettingsItemRow(
                icon = AppIcons.Shield,
                title = appText("البيانات والخصوصية", "Data & privacy"),
                onClick = { onNavigate(SettingsDestination.Privacy) }
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // System Info
            SettingsItemRow(
                icon = AppIcons.SystemMonitor,
                title = appText("معلومات النظام", "System information"),
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
            Text(appText("رجوع إلى الإعدادات", "Back to settings"))
            Icon(
                imageVector = if (LocalAppLanguage.current == AppLanguage.ARABIC) AppIcons.ChevronRight else AppIcons.ChevronLeft,
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
                        color = AppPalette.Muted
                    )
                }
            }
        }

        Icon(
            imageVector = if (LocalAppLanguage.current == AppLanguage.ARABIC) AppIcons.ChevronLeft else AppIcons.ChevronRight,
            contentDescription = null,
            tint = AppPalette.Muted,
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
                Text(text = appText("خطأ: ${uiState.message}", "Root diagnostic failed. Check root access and try again."), color = Color.Red)
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
                    text = appText("نظام إدارة الشبكات والروتر المصغر", "A compact router and network management system"),
                    fontSize = 13.sp,
                    color = AppPalette.Muted
                )

                Spacer(modifier = Modifier.height(16.dp))

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = appText("نظرة عن المشروع", "About this project"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = appText("نظام متكامل لإدارة شبكات الهوتسبوت (Hotspot) وتوليد وإنشاء كروت وبطاقات الإنترنت، وتتبع الجلسات النشطة واستهلاك البيانات للمشتركين، بالإضافة إلى التحكم بإعدادات التوجيه والجدار الناري عبر صلاحيات الـ Root.", "A complete hotspot management system for creating internet vouchers, tracking active sessions and client usage, and managing routing and firewall settings with root access."),
                    fontSize = 14.sp,
                    color = AppPalette.Muted,
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
                            text = appText("تم تطوير هذا النظام بواسطة م/محمد المدي", "Developed by Eng. Mohammad Al-Madhi"),
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
                    color = AppPalette.Muted,
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
                appText("تم رفض طلب صلاحيات الـ Root من Magisk أو تم إلغاؤه.", "Root access was denied in Magisk or the request was cancelled.")
            )
            is RootStatus.NotAvailable -> Quadruple(
                Color(0xFFFFF3E0),
                Color(0xFFE65100),
                "Root NOT AVAILABLE",
                appText(report.rootStatusMessage, "Root access is not available on this device.")
            )
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardColor),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = appText("حالة صلاحيات Root:", "Root access status:"),
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
                    color = AppPalette.Muted
                )
            }
        }

        Text(
            text = appText("تفاصيل البيئة والنظام:", "Environment and system details:"),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = DeepGreenHeader
        )

        InfoItem(appText("قاعدة البيانات المحلية (Room)", "Local database (Room)"), dbStatus)
        InfoItem(appText("نتيجة أمر التحقق 'id'", "Identity command result (id)"), report.userIdOutput)
        InfoItem(appText("النواة (Kernel)", "Kernel"), report.kernelInfo)
        InfoItem(appText("وضع SELinux", "SELinux mode"), report.selinuxMode)
        InfoItem(appText("نسخة iptables", "iptables version"), report.iptablesVersion)
        InfoItem(appText("نسخة iptables-legacy", "iptables-legacy version"), report.iptablesLegacyVersion)
        InfoItem(appText("نسخة iptables-nft", "iptables-nft version"), report.iptablesNftVersion)
        InfoItem(appText("نسخة nftables", "nftables version"), report.nftablesVersion)
        InfoItem(appText("توجيه الحزم (IP Forwarding)", "IP forwarding"), report.ipForwardingEnabled)
        InfoItem(appText("أمر Tethering الداخلي", "Internal tethering command"), report.tetheringCmdResponse)

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
                    text = appText("واجهات الشبكة المتاحة (Interfaces):", "Available network interfaces:"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DeepGreenHeader
                )
                Spacer(modifier = Modifier.height(6.dp))
                if (report.availableNetworkInterfaces.isEmpty()) {
                    Text(appText("لم يتم كشف واجهات (يتطلب Root ممنوح).", "No interfaces detected (root access required)."), fontSize = 13.sp)
                } else {
                    report.availableNetworkInterfaces.forEach { iface ->
                        Text("• $iface", fontFamily = FontFamily.Monospace, fontSize = 14.sp, color = DeepGreenHeader)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = appText("سجل الأوامر التشخيصية (Raw Logs):", "Diagnostic command log:"),
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
            Text(appText("طلب صلاحيات Root وإعادة الفحص", "Request root access and rerun diagnostics"))
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
            Text(text = title, fontSize = 13.sp, color = AppPalette.Muted)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = DeepGreenHeader)
        }
    }
}
