package com.example.mikrotik.presentation.home

import androidx.compose.animation.AnimatedVisibility
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
import com.example.mikrotik.presentation.ui.theme.AppIcons

private val DeepGreenHeader = Color(0xFF0F3822) // Rich dark green header
private val RichGreenButton = Color(0xFF00796B) // Rich emerald green buttons & icons
private val PageBackground = Color(0xFFE6F0EC)   // Distinct sage green background
private val SeamlessCardBg = Color(0xFFD8EBE3)   // Seamless flat card background (no stark white)
private val WatermarkLineColor = Color(0xFFC5E0D6) // Subtle watermark wave lines color
private val PillGreenBg = Color(0xFFC8E6C9)      // Soft green pill background
private val PillGreenDot = Color(0xFF2E7D32)     // Rich green dot

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

    var isDevicesExpanded by remember { mutableStateOf(false) }

    // Error Dialog if root command fails
    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            title = { Text("معلومات تشخيص الخطأ (Root Error)") },
            text = { Text(errorMessage ?: "") },
            confirmButton = {
                Button(onClick = { viewModel.clearError() }) {
                    Text("حسناً")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DeepGreenHeader)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Home,
                        contentDescription = "Home",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "الرئيسية",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "AL-MADDI NET",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                    }

                    IconButton(onClick = { viewModel.refreshAll() }) {
                        Icon(
                            imageVector = AppIcons.SettingsGear,
                            contentDescription = "Settings",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        },
        containerColor = PageBackground
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
                    title = "WiFi",
                    isActive = isWifiActive,
                    icon = AppIcons.WifiSignal,
                    onClick = { viewModel.toggleWifi() },
                    modifier = Modifier.weight(1f)
                )
                StatusCard(
                    title = "البيانات",
                    isActive = isDataActive,
                    icon = AppIcons.MobileDataArrows,
                    onClick = { viewModel.toggleMobileData() },
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Decorative Watermark Wave Lines inside card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                HorizontalDivider(color = WatermarkLineColor, thickness = 1.dp)
                HorizontalDivider(color = WatermarkLineColor.copy(alpha = 0.5f), thickness = 0.5f.dp)
            }

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
                    text = "استهلاك الإنترنت على الجهاز",
                    color = DeepGreenHeader,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = totalUsage,
                    color = DeepGreenHeader,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wi-Fi + بيانات الهاتف • منذ آخر إعادة تشغيل",
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
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
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
                    .background(if (isActive) PillGreenBg else Color(0xFFEEEEEE))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (isActive) PillGreenDot else Color.Gray)
                    )
                    Text(
                        text = if (isActive) "مفعل" else "معطل",
                        color = if (isActive) PillGreenDot else Color.Gray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = AppIcons.ConnectedDevices,
                    contentDescription = "Devices",
                    tint = DeepGreenHeader,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "الأجهزة المتصلة",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .border(1.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isExpanded) "▲" else "◀",
                        fontSize = 14.sp,
                        color = DeepGreenHeader
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${devices.size} جهاز متصل",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenHeader
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = AppIcons.ConnectedDevices,
                            contentDescription = "Devices",
                            tint = RichGreenButton,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                AnimatedVisibility(visible = isExpanded) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HorizontalDivider(color = WatermarkLineColor)

                        if (hotspotState !is HotspotState.Enabled) {
                            Text(
                                text = "الـ Hotspot متوقف حالياً. قم بتشغيله لاكتشاف الأجهزة المتصلة.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        } else if (devices.isEmpty()) {
                            Text(
                                text = "لا توجد أجهزة متصلة بالـ Hotspot حالياً.",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                            )
                        } else {
                            devices.forEach { device ->
                                DeviceItemRow(device = device)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DeviceItemRow(device: ConnectedDevice) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, WatermarkLineColor, RoundedCornerShape(10.dp)),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = PageBackground),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = device.deviceName ?: "جهاز متصل",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = DeepGreenHeader
                )
                Text(
                    text = device.connectionState,
                    color = PillGreenDot,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "IP: ${device.ipAddress}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = Color.DarkGray
                )
                Text(
                    text = "MAC: ${device.macAddress}",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}
