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
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel
) {
    val activeSessions by viewModel.activeSessions.collectAsState()
    val blockedMacs by viewModel.blockedMacs.collectAsState()
    val deviceUsage by viewModel.deviceUsage.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    if (actionMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearActionMessage,
            title = { Text("حالة الجهاز") },
            text = { Text(actionMessage.orEmpty()) },
            confirmButton = {
                TextButton(onClick = viewModel::clearActionMessage) { Text("حسناً") }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(
            title = "لوحة التحكم وإدارة الشبكة",
            subtitle = "AL RABDI NET",
            startIcon = AppIcons.BarChart
        )

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
                    DeviceUsageCard(
                        session = session,
                        usage = deviceUsage[session.macAddress],
                        onBlock = { viewModel.blockDevice(session.macAddress) },
                        onRename = viewModel::saveDeviceName
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
                    BlockedDeviceCard(
                        blockedMac = blockedMac,
                        displayName = viewModel.getDeviceName(blockedMac.macAddress),
                        onRename = viewModel::saveDeviceName,
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
    usage: DeviceUsageInfo?,
    onBlock: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val fallbackName = "جهاز ${session.macAddress.takeLast(5)}"
    val deviceName = usage?.displayName ?: fallbackName
    var showRenameDialog by remember(session.macAddress) { mutableStateOf(false) }
    var editedName by remember(session.macAddress) { mutableStateOf("") }
    val quotaBytes = usage?.quotaBytes ?: 0L
    val usageFraction = if (quotaBytes > 0L) {
        ((usage?.bytesUsed ?: 0L).toDouble() / quotaBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        editedName = if (deviceName == fallbackName) "" else deviceName
                        showRenameDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Text("تعديل الاسم", fontSize = 12.sp, color = RichGreenButton)
                }
                Text(
                    text = deviceName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
            }
            Text(
                text = when {
                    usage == null -> "جارٍ قراءة الاستهلاك..."
                    !usage.isMeasured -> "عداد الاستهلاك غير متاح حالياً"
                    usage.quotaBytes <= 0L -> "الاستهلاك: ${formatUsageBytes(usage.bytesUsed)}"
                    else -> "${formatUsageBytes(usage.bytesUsed)} من ${formatUsageBytes(usage.quotaBytes)}"
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.End
            )
            LinearProgressIndicator(
                progress = usageFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = when {
                    usageFraction >= 0.9f -> Color(0xFFC62828)
                    usageFraction >= 0.7f -> Color(0xFFF9A825)
                    else -> RichGreenButton
                },
                trackColor = AppPalette.Mint
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "الكود: ${session.voucherCode}",
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = RichGreenButton,
                textAlign = TextAlign.Start
            )
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("تعديل اسم الجهاز") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text("اسم الجهاز") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) onRename(session.macAddress, editedName)
                        showRenameDialog = false
                    },
                    enabled = editedName.isNotBlank()
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun DeviceUsageCard(
    session: ActiveSessionEntity,
    usage: DeviceUsageInfo?,
    onBlock: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val fallbackName = "جهاز ${session.macAddress.takeLast(5)}"
    val deviceName = usage?.displayName ?: fallbackName
    val quotaBytes = usage?.quotaBytes ?: 0L
    val usedBytes = (usage?.bytesUsed ?: 0L).coerceAtLeast(0L)
    val usageFraction = if (quotaBytes > 0L) {
        (usedBytes.toDouble() / quotaBytes.toDouble()).toFloat().coerceIn(0f, 1f)
    } else 0f
    val usageColor = when {
        usageFraction >= 0.9f -> Color(0xFFC62828)
        usageFraction >= 0.7f -> Color(0xFFF9A825)
        else -> RichGreenButton
    }

    var showRenameDialog by remember(session.macAddress) { mutableStateOf(false) }
    var showBlockDialog by remember(session.macAddress) { mutableStateOf(false) }
    var editedName by remember(session.macAddress) { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(AppPalette.Mint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.SystemMonitor,
                        contentDescription = "جهاز متصل",
                        tint = RichGreenButton,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = deviceName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "IP: ${session.ipAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "MAC: ${session.macAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(color = WatermarkLineColor, thickness = 0.7.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = usageFraction,
                            modifier = Modifier.size(96.dp),
                            color = usageColor,
                            trackColor = AppPalette.Mint,
                            strokeWidth = 8.dp
                        )
                        Text(
                            text = if (quotaBytes > 0L) "${(usageFraction * 100).toInt()}%" else "—",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepGreenHeader
                        )
                    }
                    Text(
                        text = when {
                            usage == null -> "جارٍ القياس"
                            quotaBytes > 0L -> "${formatUsageBytes(usedBytes)} / ${formatUsageBytes(quotaBytes)}"
                            usage.isMeasured -> formatUsageBytes(usedBytes)
                            else -> "غير متاح"
                        },
                        fontSize = 10.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            editedName = if (deviceName == fallbackName) "" else deviceName
                            showRenameDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "تعديل الاسم",
                            modifier = Modifier.size(17.dp),
                            tint = RichGreenButton
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الاسم", fontSize = 12.sp, color = RichGreenButton)
                    }

                    OutlinedButton(
                        onClick = { showBlockDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 13.dp, vertical = 7.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Icon(
                            imageVector = AppIcons.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text("حظر", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("تعديل اسم الجهاز") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text("اسم الجهاز") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) onRename(session.macAddress, editedName)
                        showRenameDialog = false
                    },
                    enabled = editedName.isNotBlank()
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("إلغاء") }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text("حظر الجهاز؟") },
            text = {
                Text("سيتم قطع الإنترنت عن الجهاز وحذف البطاقة المتصل بها. عند رفع الحظر سيحتاج إلى تسجيل الدخول ببطاقة جديدة.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        onBlock()
                    }
                ) { Text("حظر وحذف البطاقة", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text("إلغاء") }
            }
        )
    }
}

private fun formatUsageBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    bytes >= 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    bytes >= 1024L -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "$bytes B"
}

@Composable
private fun BlockedDeviceCard(
    blockedMac: BlockedMacEntity,
    displayName: String,
    onRename: (String, String) -> Unit,
    onUnblock: () -> Unit
) {
    var showRenameDialog by remember(blockedMac.macAddress) { mutableStateOf(false) }
    var editedName by remember(blockedMac.macAddress) { mutableStateOf(displayName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(22.dp)),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(17.dp))
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.SystemMonitor,
                        contentDescription = "جهاز محظور",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(34.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "MAC: ${blockedMac.macAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = blockedMac.reason,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 11.sp,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFFCDD2), thickness = 0.7.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828))
                    )
                    Text("محظور", fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(
                        onClick = {
                            editedName = displayName
                            showRenameDialog = true
                        },
                        contentPadding = PaddingValues(horizontal = 5.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = "تعديل الاسم",
                            modifier = Modifier.size(17.dp),
                            tint = RichGreenButton
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("الاسم", fontSize = 12.sp, color = RichGreenButton)
                    }
                    Button(
                        onClick = onUnblock,
                        colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 15.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Login,
                            contentDescription = null,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("رفع الحظر", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("تعديل اسم الجهاز") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text("اسم الجهاز") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) onRename(blockedMac.macAddress, editedName)
                        showRenameDialog = false
                    },
                    enabled = editedName.isNotBlank()
                ) { Text("حفظ") }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text("إلغاء") }
            }
        )
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
