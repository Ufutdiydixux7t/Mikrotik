package com.example.mikrotik.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import com.example.mikrotik.domain.model.ConnectedDevice
import com.example.mikrotik.domain.model.HotspotState
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.components.AppWaveDecoration
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border
private val PillGreenBg get() = AppPalette.MintStrong
private val PillGreenDot get() = AppPalette.Positive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onOpenDiagnostics: () -> Unit = {}
) {
    val hotspotState by viewModel.hotspotState.collectAsState()
    val connectedDevices by viewModel.connectedDevices.collectAsState()
    val isWifiActive by viewModel.isWifiActive.collectAsState()
    val isDataActive by viewModel.isDataActive.collectAsState()
    val totalUsage by viewModel.totalUsageFormatted.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isDevicesExpanded by remember { mutableStateOf(true) }

    // Error Dialog if root command fails
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text(appText("معلومات تشخيص الخطأ (Root Error)", "Root error details")) },
            text = { Text(appText(errorMessage ?: "", "A root command failed. Open System information for diagnostics.")) },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text(appText("حسناً", "OK"))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = appText("الرئيسية", "Home"),
                subtitle = "AL RABDI NET",
                startIcon = AppIcons.Home,
                endIcon = AppIcons.Menu,
                onEndClick = onOpenDiagnostics
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Total Usage Seamless Card with Watermark Lines
            TotalUsageCard(totalUsage = totalUsage)

            // 2. WiFi, Mobile Data and Hotspot Status Cards Row (Side by side)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusCard(
                    title = appText("البيانات", "Mobile data"),
                    isActive = isDataActive,
                    icon = AppIcons.MobileDataArrows,
                    onClick = { viewModel.toggleMobileData() },
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "WiFi",
                    isActive = isWifiActive,
                    icon = AppIcons.WifiSignal,
                    onClick = { viewModel.toggleWifi() },
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "Hotspot",
                    isActive = hotspotState is HotspotState.Enabled,
                    icon = AppIcons.AntennaHotspot,
                    onClick = { viewModel.toggleHotspot() },
                    modifier = Modifier.weight(1f)
                )
            }

            // 3. Connected Devices Section (Seamless)
            ConnectedDevicesSection(
                devices = connectedDevices,
                isExpanded = isDevicesExpanded,
                onToggleExpand = { isDevicesExpanded = !isDevicesExpanded },
                hotspotState = hotspotState
            )
        }
    }
}

@Composable
fun TotalUsageCard(totalUsage: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            AppWaveDecoration(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(70.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = AppIcons.AntennaHotspot,
                    contentDescription = "Antenna",
                    tint = RichGreenButton,
                    modifier = Modifier.size(42.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = appText("استهلاك الإنترنت الكلي", "Total internet usage"),
                    color = DeepGreenHeader,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when (totalUsage) {
                        "جارٍ القياس" -> appText("جارٍ القياس", "Measuring…")
                        "غير متاح" -> appText("غير متاح", "Not available")
                        else -> totalUsage
                    },
                    color = DeepGreenHeader,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = appText("Wi-Fi + بيانات الهاتف • منذ آخر إعادة تشغيل", "Wi-Fi + mobile data • since last restart"),
                    color = DeepGreenHeader.copy(alpha = 0.72f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    isActive: Boolean,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clickable { onClick() }
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = RichGreenButton,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isActive) PillGreenBg else AppPalette.Border)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (isActive) appText("مفعل", "On") else appText("متوقف", "Off"),
                        color = if (isActive) PillGreenDot else AppPalette.Muted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActive) PillGreenDot else AppPalette.Muted)
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectedDevicesSection(
    devices: List<ConnectedDevice>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    hotspotState: HotspotState
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(bottom = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.ConnectedDevices,
                    contentDescription = appText("الأجهزة المتصلة", "Connected devices"),
                    tint = RichGreenButton,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = appText("الأجهزة المتصلة", "Connected devices"),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${devices.size}",
                    fontSize = 13.sp,
                    color = AppPalette.Muted
                )
                Icon(
                    imageVector = if (isExpanded) AppIcons.ChevronRight else AppIcons.ChevronLeft,
                    contentDescription = if (isExpanded) appText("طي القائمة", "Collapse list") else appText("عرض الأجهزة", "Show devices"),
                    tint = DeepGreenHeader,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (isExpanded) {
            when {
                hotspotState is HotspotState.Disabled -> Text(
                    text = appText("نقطة الاتصال متوقفة؛ شغّلها لعرض الأجهزة المتصلة فعلياً.", "Hotspot is off. Turn it on to see connected devices."),
                    fontSize = 13.sp,
                    color = AppPalette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                hotspotState is HotspotState.Enabling -> Text(
                    text = appText("جارٍ تحديث حالة نقطة الاتصال والأجهزة…", "Refreshing hotspot and device status…"),
                    fontSize = 13.sp,
                    color = AppPalette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                hotspotState is HotspotState.Error -> Text(
                    text = appText(hotspotState.message, "Could not read hotspot status. Check root access and hotspot permissions."),
                    fontSize = 13.sp,
                    color = AppPalette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                devices.isEmpty() -> Text(
                    text = appText("لا توجد أجهزة متصلة بالـ Hotspot حالياً.", "No devices are connected to the hotspot."),
                    fontSize = 13.sp,
                    color = AppPalette.Muted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
                else -> devices.forEach { device -> DeviceItemRow(device = device) }
            }
        }
    }
}

@Composable
fun DeviceItemRow(device: ConnectedDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = AppIcons.Smartphone,
                contentDescription = appText("هاتف متصل", "Connected device"),
                tint = DeepGreenHeader,
                modifier = Modifier.size(34.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = device.deviceName ?: appText("جهاز متصل", "Connected device"),
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = DeepGreenHeader
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = device.ipAddress,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AppPalette.Muted
                    )
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(PillGreenDot)
                    )
                    Text(
                        text = if (device.connectionState.equals("CONNECTED", ignoreCase = true)) appText("متصل", "Connected") else device.connectionState,
                        fontSize = 12.sp,
                        color = PillGreenDot,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
