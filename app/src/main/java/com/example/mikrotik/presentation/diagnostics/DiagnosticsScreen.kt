package com.example.mikrotik.presentation.diagnostics

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.core.captive.PortalTheme
import com.example.mikrotik.core.root.RootStatus
import com.example.mikrotik.core.root.SystemDiagnosticReport
import com.example.mikrotik.security.AppLockSettingsScreen
import com.example.mikrotik.security.AppLockViewModel
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

@Composable
private fun settingsSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = Color.White,
    checkedTrackColor = AppPalette.Positive,
    checkedBorderColor = AppPalette.Positive,
    uncheckedThumbColor = if (AppPalette.isDarkMode) Color(0xFFDCE7EB) else Color.White,
    uncheckedTrackColor = if (AppPalette.isDarkMode) Color(0xFF344A53) else Color(0xFFE1E9EC),
    uncheckedBorderColor = if (AppPalette.isDarkMode) Color(0xFF78909C) else Color(0xFF9AAAB1)
)

sealed class SettingsDestination {
    object MainSettings : SettingsDestination()
    object SystemInfo : SettingsDestination()
    object Measurement : SettingsDestination()
    object LoginPage : SettingsDestination()
    object AboutUs : SettingsDestination()
    object Privacy : SettingsDestination()
    object Language : SettingsDestination()
    object AppLock : SettingsDestination()
}

@Composable
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel,
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onUsageReset: () -> Unit,
    appLockViewModel: AppLockViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    var currentDestination by remember { mutableStateOf<SettingsDestination>(SettingsDestination.MainSettings) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AppTopBar(
            title = when (currentDestination) {
                SettingsDestination.MainSettings -> appText("الإعدادات", "Settings")
                SettingsDestination.SystemInfo -> appText("تشخيصات الروت والنظام", "Root & System Diagnostics")
                SettingsDestination.Measurement -> appText("القياس", "Usage measurement")
                SettingsDestination.LoginPage -> appText("واجهة تسجيل الدخول", "Login portal")
                SettingsDestination.AboutUs -> appText("نظرة عنا", "About")
                SettingsDestination.Privacy -> appText("خصوصية الاستخدام", "Usage privacy")
                SettingsDestination.Language -> appText("اللغة", "Language")
                SettingsDestination.AppLock -> appText("قفل التطبيق", "App lock")
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
                        viewModel = viewModel,
                        appLockViewModel = appLockViewModel,
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
                    PrivacySettingsScreen(
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
                SettingsDestination.AppLock -> {
                    AppLockSettingsScreen(
                        viewModel = appLockViewModel,
                        onBack = { currentDestination = SettingsDestination.MainSettings }
                    )
                }
            }
        }
    }
}

@Composable
fun MainSettingsList(
    viewModel: DiagnosticsViewModel,
    appLockViewModel: AppLockViewModel,
    isDarkModeEnabled: Boolean,
    onDarkModeToggle: (Boolean) -> Unit,
    language: AppLanguage,
    onNavigate: (SettingsDestination) -> Unit
) {
    val phoneInternetBlock by viewModel.phoneInternetBlock.collectAsState()
    val portalAppearance by viewModel.portalAppearance.collectAsState()
    val appLockConfig by appLockViewModel.config.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    var showInternetBlockConfirmation by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // 1. قسم الأمان والوصول
        SettingsSectionHeader(appText("الأمان والوصول", "Security & Access"))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // App Lock Row
                SettingsItemRow(
                    icon = AppIcons.Shield,
                    iconContainerColor = if (appLockConfig.enabled) AppPalette.MintStrong else AppPalette.Mint,
                    iconTintColor = AppPalette.Positive,
                    title = appText("قفل التطبيق", "App lock"),
                    subtitle = if (appLockConfig.enabled) {
                        appText("مفعّل • ${appLockConfig.userNames.size} مستخدم", "Enabled • ${appLockConfig.userNames.size} user(s)")
                    } else {
                        appText("غير مفعّل • حماية الدخول برمز PIN", "Disabled • Protect app access with a PIN")
                    },
                    badgeText = if (appLockConfig.enabled) appText("مفعّل", "Active") else null,
                    badgeColor = AppPalette.Positive,
                    onClick = { onNavigate(SettingsDestination.AppLock) }
                )

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                // Block Phone Apps Internet Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Block,
                            contentDescription = appText("حظر إنترنت تطبيقات الهاتف", "Block phone apps' Internet"),
                            tint = if (phoneInternetBlock.isEnabled) Color(0xFFC62828) else RichGreenButton,
                            modifier = Modifier.size(23.dp)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = appText("حظر إنترنت تطبيقات الهاتف", "Block phone apps' Internet"),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DeepGreenHeader
                                )
                            }
                            Text(
                                text = appText(
                                    "يستثني MikroTik وDNS والشبكة المحلية دون إيقاف إنترنت المشتركين.",
                                    "Exempts MikroTik, DNS & local LAN without interrupting hotspot clients."
                                ),
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                color = AppPalette.Muted
                            )
                        }
                    }
                    Switch(
                        checked = phoneInternetBlock.isEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) showInternetBlockConfirmation = true
                            else viewModel.setPhoneAppsInternetBlocked(false)
                        },
                        enabled = !phoneInternetBlock.isLoading,
                        colors = settingsSwitchColors()
                    )
                }

                if (phoneInternetBlock.failed) {
                    Text(
                        text = appText(
                            "تعذر التحقق من قواعد الروت أو تطبيقها. لم يتم تأكيد الحظر.",
                            "Could not verify or apply root firewall rules; blocking is not confirmed."
                        ),
                        color = Color(0xFFB3261E),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 2. قسم إدارة الشبكة والبوابة
        SettingsSectionHeader(appText("إدارة الشبكة والبوابة", "Hotspot & Network Management"))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Usage Measurement
                SettingsItemRow(
                    icon = AppIcons.BarChart,
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("القياس ومراقبة الاستهلاك", "Usage measurement"),
                    subtitle = appText("إجمالي المصدر والأجهزة المتصلة واستهلاك الهاتف", "Source, connected devices, and phone data breakdown"),
                    onClick = { onNavigate(SettingsDestination.Measurement) }
                )

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                // Login Portal
                val themeColorName = when (portalAppearance.theme) {
                    PortalTheme.GREEN -> appText("أخضر", "Green")
                    PortalTheme.BLUE -> appText("أزرق", "Blue")
                    PortalTheme.RED -> appText("أحمر", "Red")
                    PortalTheme.PINK -> appText("وردي", "Pink")
                    PortalTheme.PURPLE -> appText("بنفسجي", "Purple")
                    PortalTheme.ORANGE -> appText("برتقالي", "Orange")
                }
                SettingsItemRow(
                    icon = AppIcons.Login,
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("واجهة تسجيل الدخول", "Login portal appearance"),
                    subtitle = appText(
                        "الاسم الحالي: ${portalAppearance.name} • اللون: $themeColorName",
                        "Name: ${portalAppearance.name} • Theme: $themeColorName"
                    ),
                    onClick = { onNavigate(SettingsDestination.LoginPage) }
                )
            }
        }

        // 3. قسم التفضيلات الشخصية
        SettingsSectionHeader(appText("التفضيلات الشخصية", "Personal Preferences"))
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
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("اللغة", "Language"),
                    subtitle = language.nativeName,
                    badgeText = language.code.uppercase(),
                    badgeColor = RichGreenButton,
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
                            contentDescription = appText("الوضع الداكن", "Dark mode"),
                            tint = RichGreenButton,
                            modifier = Modifier.size(23.dp)
                        )
                        Column {
                            Text(
                                text = appText("الوضع الداكن", "Dark mode"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepGreenHeader
                            )
                            Text(
                                text = if (isDarkModeEnabled) appText("مفعّل", "Enabled") else appText("معطل", "Disabled"),
                                fontSize = 11.sp,
                                color = AppPalette.Muted
                            )
                        }
                    }
                    Switch(
                        checked = isDarkModeEnabled,
                        onCheckedChange = onDarkModeToggle,
                        colors = settingsSwitchColors()
                    )
                }
            }
        }

        // 4. قسم التشخيص والدعم
        SettingsSectionHeader(appText("التشخيص والدعم", "System & Diagnostics"))
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // System Diagnostics
                val rootSubtitle = when (val state = uiState) {
                    is DiagnosticsUiState.Success -> when (state.report.rootStatus) {
                        is RootStatus.Granted -> appText("الروت: ممنوح (uid=0) • iptables وقاعدة البيانات متصلة", "Root: Granted (uid=0) • iptables & DB connected")
                        is RootStatus.Denied -> appText("الروت: مرفوض من Magisk", "Root: Denied in Magisk")
                        is RootStatus.NotAvailable -> appText("الروت: غير متوفر على الجهاز", "Root: Not available")
                    }
                    is DiagnosticsUiState.Loading -> appText("جاري فحص بيئة النظام ورصده...", "Checking system environment...")
                    is DiagnosticsUiState.Error -> appText("فحص الروت وقواعد البيانات", "Root and database diagnostics")
                }
                SettingsItemRow(
                    icon = AppIcons.SystemMonitor,
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("تشخيصات الروت والنظام", "Root & System Diagnostics"),
                    subtitle = rootSubtitle,
                    onClick = { onNavigate(SettingsDestination.SystemInfo) }
                )

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                // Privacy
                SettingsItemRow(
                    icon = AppIcons.Info,
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("خصوصية الاستخدام", "Usage privacy"),
                    subtitle = appText("بيانات الشبكة والتخزين والتحكم المحلي", "Network data, storage, and local controls"),
                    onClick = { onNavigate(SettingsDestination.Privacy) }
                )

                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

                // About Us
                SettingsItemRow(
                    icon = AppIcons.Info,
                    iconContainerColor = AppPalette.Mint,
                    iconTintColor = RichGreenButton,
                    title = appText("عن التطبيق", "About"),
                    subtitle = appText("نظام Mini MikroTik وإصدار المطور", "Mini MikroTik system & developer info"),
                    onClick = { onNavigate(SettingsDestination.AboutUs) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showInternetBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showInternetBlockConfirmation = false },
            title = {
                Text(appText("تأكيد حظر إنترنت تطبيقات الهاتف", "Confirm blocking phone apps' Internet"))
            },
            text = {
                Text(
                    appText(
                        "سيُمنع الإنترنت عن التطبيقات الأخرى وخدمات النظام. يبقى MikroTik والأجهزة المتصلة بالهوتسبوت مستثناة. قد تبقى قواعد الروت فعالة حتى توقف المفتاح أو تعيد تشغيل الهاتف؛ أوقف الحظر قبل حذف التطبيق.",
                        "Other apps and system services will lose Internet access. MikroTik and hotspot clients remain exempt. Root rules may stay active until you switch this off or reboot; turn it off before uninstalling the app."
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showInternetBlockConfirmation = false
                        viewModel.setPhoneAppsInternetBlocked(true)
                    }
                ) {
                    Text(appText("تفعيل الحظر", "Enable blocking"), color = Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showInternetBlockConfirmation = false }) {
                    Text(appText("إلغاء", "Cancel"), color = DeepGreenHeader)
                }
            },
            containerColor = SeamlessCardBg
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = RichGreenButton,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
@Suppress("UNUSED_PARAMETER")
fun SettingsItemRow(
    icon: ImageVector,
    iconContainerColor: Color = AppPalette.Mint,
    iconTintColor: Color = RichGreenButton,
    title: String,
    subtitle: String? = null,
    badgeText: String? = null,
    badgeColor: Color = RichGreenButton,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTintColor,
                modifier = Modifier.size(22.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenHeader
                    )
                    if (badgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(badgeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = badgeText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = badgeColor
                            )
                        }
                    }
                }
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
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
private fun LanguageSettingsScreen(
    language: AppLanguage,
    onLanguageChange: (AppLanguage) -> Unit,
    onBack: () -> Unit
) {
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
                            .background(if (language == option) AppPalette.Mint.copy(alpha = 0.45f) else Color.Transparent)
                            .border(
                                1.dp,
                                if (language == option) RichGreenButton.copy(alpha = 0.45f) else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
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
        PortalTheme.RED to Color(0xFFD84343),
        PortalTheme.PINK to Color(0xFFC2185B),
        PortalTheme.PURPLE to Color(0xFF6B4FA1),
        PortalTheme.ORANGE to Color(0xFFB45309)
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
                PortalTheme.values().toList().chunked(2).forEach { themes ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        themes.forEach { theme ->
                            val selected = appearance.theme == theme
                            val accent = colors.getValue(theme)
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) accent.copy(alpha = 0.08f) else SeamlessCardBg)
                                    .border(
                                        1.dp,
                                        if (selected) accent else WatermarkLineColor,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable { viewModel.updatePortalTheme(theme) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(9.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(accent, RoundedCornerShape(50))
                                )
                                Text(
                                    text = when (theme) {
                                        PortalTheme.GREEN -> appText("أخضر", "Green")
                                        PortalTheme.BLUE -> appText("أزرق", "Blue")
                                        PortalTheme.RED -> appText("أحمر", "Red")
                                        PortalTheme.PINK -> appText("وردي", "Pink")
                                        PortalTheme.PURPLE -> appText("بنفسجي", "Purple")
                                        PortalTheme.ORANGE -> appText("برتقالي", "Orange")
                                    },
                                    modifier = Modifier.weight(1f),
                                    color = if (selected) accent else DeepGreenHeader,
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                        if (themes.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
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
    val context = LocalContext.current
    val cannotOpenContact = appText("لا يوجد تطبيق متاح لفتح وسيلة التواصل هذه.", "No app is available to open this contact method.")
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
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
                modifier = Modifier.fillMaxWidth().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = appText("عن المشروع", "About the project"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Text(
                    text = "Mini MikroTik",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Bold,
                    color = RichGreenButton
                )
                Text(
                    text = appText(
                        "نظام متكامل لإدارة شبكات الهوتسبوت (Hotspot)، وإنشاء بطاقات الإنترنت، ومتابعة الجلسات النشطة واستهلاك البيانات، إلى جانب إدارة إعدادات التوجيه والجدار الناري.",
                        "A complete hotspot management system for creating internet vouchers, tracking active sessions and data usage, and managing routing and firewall settings."
                    ),
                    fontSize = 14.sp,
                    color = AppPalette.Muted,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Start
                )

                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = appText("عني", "About me"),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Text(
                    text = appText("م/ محمد المدي", "Eng. Mohammad Al-Madhi"),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = DeepGreenHeader
                )
                Text(
                    text = appText(
                        "مطور نظام Mini MikroTik.",
                        "Developer of the Mini MikroTik system."
                    ),
                    fontSize = 13.sp,
                    lineHeight = 20.sp,
                    color = AppPalette.Muted
                )

                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ContactIconButton(
                        icon = AppIcons.Facebook,
                        iconColor = Color(0xFF1877F2),
                        contentDescription = appText("فيسبوك", "Facebook"),
                        uri = "https://www.facebook.com/profile.php?id=100043079018929",
                        unavailableMessage = cannotOpenContact,
                        context = context
                    )
                    ContactIconButton(
                        icon = AppIcons.Email,
                        iconColor = Color(0xFF607D8B),
                        contentDescription = appText("البريد الإلكتروني", "Email"),
                        uri = "mailto:doctormohammed7788@gmail.com",
                        unavailableMessage = cannotOpenContact,
                        context = context
                    )
                    ContactIconButton(
                        icon = AppIcons.WhatsApp,
                        iconColor = Color(0xFF25D366),
                        contentDescription = appText("واتساب", "WhatsApp"),
                        uri = "https://wa.me/201031697230",
                        unavailableMessage = cannotOpenContact,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactIconButton(
    icon: ImageVector,
    iconColor: Color,
    contentDescription: String,
    uri: String,
    unavailableMessage: String,
    context: Context
) {
    Surface(
        onClick = { openContactTarget(context, uri, unavailableMessage) },
        modifier = Modifier.size(38.dp),
        shape = RoundedCornerShape(50),
        color = iconColor
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = Color.White,
                modifier = Modifier.size(19.dp)
            )
        }
    }
}

private fun openContactTarget(context: Context, uri: String, unavailableMessage: String) {
    try {
        val contactUri = Uri.parse(uri)
        val action = if (contactUri.scheme == "mailto") Intent.ACTION_SENDTO else Intent.ACTION_VIEW
        context.startActivity(Intent(action, contactUri))
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, unavailableMessage, Toast.LENGTH_SHORT).show()
    }
}

@Composable
private fun PrivacySettingsScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        TextButton(
            onClick = onBack,
            modifier = Modifier.align(Alignment.Start)
        ) {
            Text(
                text = appText("رجوع إلى الإعدادات", "Back to settings"),
                color = DeepGreenHeader
            )
        }

        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = appText("خصوصية الاستخدام", "Usage privacy"),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )
            Text(
                text = appText(
                    "ملخص واضح للبيانات التي يديرها التطبيق وكيفية التحكم بها.",
                    "A clear summary of the data the app manages and how you control it."
                ),
                fontSize = 13.sp,
                lineHeight = 20.sp,
                color = AppPalette.Muted
            )
        }

        PrivacyInfoSection(
            title = appText("ما البيانات التي يعالجها التطبيق؟", "What data does the app handle?"),
            message = appText(
                "قد يعالج التطبيق رموز البطاقات وحدودها ومددها، أسماء الأجهزة وعناوين MAC أو عناوين الشبكة، وعدادات استهلاك الجلسات وتفضيلات الإعدادات.",
                "The app may handle voucher codes, limits and durations; device names and MAC or network addresses; session-usage counters; and app preferences."
            )
        )

        PrivacyInfoSection(
            title = appText("لماذا تُستخدم هذه البيانات؟", "Why is this data used?"),
            message = appText(
                "للتحقق من البطاقات، تطبيق حدود الصلاحية والاستهلاك، عرض الأجهزة المتصلة، إدارة الحظر، وتقديم ملخصات الاستخدام.",
                "To validate vouchers, enforce time and data limits, show connected devices, manage blocking, and display usage summaries."
            )
        )

        PrivacyInfoSection(
            title = appText("التخزين وصلاحيات الروت", "Storage and root access"),
            message = appText(
                "تُحفظ بيانات التشغيل ضمن مساحة التطبيق على هذا الهاتف. تحتاج بعض وظائف مراقبة الشبكة والتحكم بها إلى صلاحية Root لقراءة العدادات وتطبيق قواعد الهوتسبوت والجدار الناري؛ امنحها فقط إذا كنت تثق بالتطبيق.",
                "Operational data is kept in the app's storage on this phone. Some network-monitoring and control features use root to read counters and apply hotspot/firewall rules; grant root only if you trust the app."
            )
        )

        PrivacyInfoSection(
            title = appText("المشاركة وحركة الإنترنت", "Sharing and Internet traffic"),
            message = appText(
                "لا توفر وظائف التطبيق الحالية مزامنة سحابية لبيانات البطاقات أو سجلاً لصفحات التصفح؛ القياس يعرض أحجام حركة الشبكة. قد تدخل بيانات التطبيق ضمن النسخ الاحتياطية في Android بحسب إعدادات جهازك. تبقى حركة الإنترنت خاضعة لمزود الخدمة والمواقع أو التطبيقات التي تستخدمها.",
                "The app's current features do not provide cloud syncing of voucher data or a browsing-history log; usage measurement reports traffic volumes. Android device-backup settings may include app data. Internet traffic remains subject to your provider and the sites or apps you use."
            )
        )

        PrivacyInfoSection(
            title = appText("التحكم والحذف", "Controls and deletion"),
            message = appText(
                "يمكنك حذف البطاقات من شاشة البطاقات، وتصفير أرقام القياس من صفحة القياس، وتعديل أسماء الأجهزة من لوحة التحكم. أوقف حظر إنترنت الهاتف قبل إزالة التطبيق.",
                "You can delete vouchers from the voucher screen, reset counters from Usage measurement, and edit device names from the dashboard. Turn off phone Internet blocking before uninstalling the app."
            )
        )

        Text(
            text = appText(
                "قد تختلف طريقة عرض عدادات الشبكة حسب إصدار Android والجهاز ومزوّد الخدمة.",
                "Network-counter behavior may vary by Android version, device, and Internet provider."
            ),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            fontSize = 11.sp,
            lineHeight = 17.sp,
            color = AppPalette.Muted
        )
    }
}

@Composable
private fun PrivacyInfoSection(title: String, message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepGreenHeader
        )
        Text(
            text = message,
            fontSize = 13.sp,
            lineHeight = 21.sp,
            color = AppPalette.Muted
        )
    }
    HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)
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
