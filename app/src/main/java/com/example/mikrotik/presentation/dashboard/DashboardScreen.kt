package com.example.mikrotik.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.data.db.entity.ActiveSessionEntity
import com.example.mikrotik.data.db.entity.BlockedMacEntity
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.theme.AppIcons

private val DeepGreenHeader = Color(0xFF0F3822)
private val RichGreenButton = Color(0xFF00796B)
private val PageBackground = Color(0xFFE6F0EC)
private val SeamlessCardBg = Color(0xFFFFFFFF)
private val WatermarkLineColor = Color(0xFFC5E0D6)

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val activeSessions by viewModel.activeSessions.collectAsState()
    val blockedMacs by viewModel.blockedMacs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        AppTopBar(title = "لوحة التحكم وإدارة الشبكة", startIcon = AppIcons.BarChart)

        // Content Area
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WatermarkLineColor, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${activeSessions.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RichGreenButton
                        )
                        Text(
                            text = "الجلسات الفعالة",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(WatermarkLineColor)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${blockedMacs.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = "الأجهزة المحظورة",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Section 1: Active Sessions
            Text(
                text = "الجلسات النشطة المباشرة",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )

            if (activeSessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "لا توجد جلسات نشطة حالياً. بمجرد تسجيل دخول العملاء ستظهر تفاصيلهم هنا.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                activeSessions.forEach { session ->
                    ActiveSessionCard(
                        session = session,
                        onBlock = { viewModel.blockDevice(session.macAddress) }
                    )
                }
            }

            // Section 2: Blocked Devices
            Text(
                text = "قائمة الأجهزة المحظورة",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )

            if (blockedMacs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "لا توجد أجهزة محظورة حالياً.",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                blockedMacs.forEach { blockedMac ->
                    BlockedMacCard(
                        blockedMac = blockedMac,
                        onUnblock = { viewModel.unblockDevice(blockedMac.macAddress) }
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveSessionCard(
    session: ActiveSessionEntity,
    onBlock: () -> Unit
) {
    val totalMb = (session.bytesDownloaded + session.bytesUploaded) / (1024 * 1024)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("حظر", fontSize = 12.sp, color = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "IP: ${session.ipAddress}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = DeepGreenHeader
                        )
                        Text(
                            text = "MAC: ${session.macAddress}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.ConnectedDevices,
                            contentDescription = "Device",
                            tint = RichGreenButton,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "الكود: ${session.voucherCode}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = RichGreenButton
                )
                Text(
                    text = "الاستهلاك: $totalMb MB",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
            }
        }
    }
}

@Composable
fun BlockedMacCard(
    blockedMac: BlockedMacEntity,
    onUnblock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("إلغاء الحظر", fontSize = 12.sp, color = Color.White)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MAC: ${blockedMac.macAddress}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFFC62828)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = blockedMac.reason,
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )
            }
        }
    }
}
