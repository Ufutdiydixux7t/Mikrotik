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
import com.example.mikrotik.presentation.ui.localization.AppLanguage
import com.example.mikrotik.presentation.ui.localization.LocalAppLanguage
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border
private fun String.canonicalMac(): String = trim().lowercase()

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    language: AppLanguage
) {
    CompositionLocalProvider(LocalAppLanguage provides language) {
        DashboardContent(viewModel)
    }
}

@Composable
private fun DashboardContent(viewModel: DashboardViewModel) {
    val activeSessions by viewModel.activeSessions.collectAsState()
    val blockedMacs by viewModel.blockedMacs.collectAsState()
    val deviceUsage by viewModel.deviceUsage.collectAsState()
    val blockedVoucherCodes by viewModel.blockedVoucherCodes.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()
    val uniqueBlockedMacs = blockedMacs.distinctBy { it.macAddress.canonicalMac() }
    val blockedMacKeys = uniqueBlockedMacs.mapTo(mutableSetOf()) { it.macAddress.canonicalMac() }
    val visibleActiveSessions = activeSessions
        .distinctBy { it.macAddress.canonicalMac() }
        .filterNot { it.macAddress.canonicalMac() in blockedMacKeys }

    if (actionMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::clearActionMessage,
            title = { Text(appText("حالة الجهاز", "Device status")) },
            text = { Text(localizeDeviceAction(actionMessage.orEmpty())) },
            confirmButton = {
                TextButton(onClick = viewModel::clearActionMessage) { Text(appText("حسناً", "OK")) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(
            title = appText("لوحة التحكم وإدارة الشبكة", "Network dashboard"),
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
                            text = "${visibleActiveSessions.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RichGreenButton
                        )
                        Text(
                            text = appText("الجلسات الفعالة", "Active sessions"),
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
                            text = "${uniqueBlockedMacs.size}",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = appText("الأجهزة المحظورة", "Blocked devices"),
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Section 1: Active Sessions
            Text(
                text = appText("الجلسات النشطة المباشرة", "Live active sessions"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )

            if (visibleActiveSessions.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = appText("لا توجد جلسات نشطة حالياً. بمجرد تسجيل دخول العملاء ستظهر تفاصيلهم هنا.", "No active sessions. Client details will appear here after they sign in."),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                visibleActiveSessions.forEach { session ->
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
                text = appText("قائمة الأجهزة المحظورة", "Blocked devices"),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )

            if (uniqueBlockedMacs.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = appText("لا توجد أجهزة محظورة حالياً.", "There are no blocked devices."),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                uniqueBlockedMacs.forEach { blockedMac ->
                    BlockedDeviceCard(
                        blockedMac = blockedMac,
                        displayName = viewModel.getDeviceName(blockedMac.macAddress, appText("جهاز ${blockedMac.macAddress.takeLast(5)}", "Device ${blockedMac.macAddress.takeLast(5)}")),
                        voucherCode = blockedVoucherCodes[blockedMac.macAddress.canonicalMac()],
                        onRename = viewModel::saveDeviceName,
                        onUnblock = { viewModel.unblockDevice(blockedMac.macAddress) }
                    )
                }
            }
        }
    }
}

@Composable
private fun localizeDeviceAction(message: String): String = when {
    message.startsWith("تم حظر الجهاز") -> appText(
        message,
        "Device blocked: internet was cut off and the active voucher was deleted. A new voucher is required after unblocking."
    )
    message.startsWith("سُجل الجهاز") -> appText(
        message,
        "The device was added to the block list, but some network rules could not be applied. Keep the hotspot on and retry."
    )
    message.startsWith("تعذر حظر الجهاز") -> appText(
        message,
        "Could not block the device: ${message.substringAfter(':').trim()}"
    )
    message.startsWith("لم يكتمل رفع الحظر") -> appText(
        message,
        "Could not finish unblocking because network rules could not be updated. Check root access and hotspot status, then retry."
    )
    message.startsWith("تم رفع الحظر. سيُعاد فحص البوابة") -> appText(
        message,
        "Device unblocked. It will be asked to sign in again with a new voucher."
    )
    message.startsWith("تم رفع الحظر، لكن") -> appText(
        message,
        "Device unblocked, but reconnecting it could not be forced. It will need to sign in when it next opens the internet."
    )
    message.startsWith("تعذر رفع الحظر") -> appText(
        message,
        "Could not unblock the device: ${message.substringAfter(':').trim()}"
    )
    else -> appText(message, "Device action completed.")
}

@Composable
fun ActiveSessionCard(
    session: ActiveSessionEntity,
    usage: DeviceUsageInfo?,
    onBlock: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val fallbackName = appText("جهاز ${session.macAddress.takeLast(5)}", "Device ${session.macAddress.takeLast(5)}")
    val deviceName = usage?.displayName?.takeIf { it.isNotBlank() } ?: fallbackName
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
                    Text(appText("حظر", "Block"), fontSize = 12.sp, color = Color.White)
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
                    Text(appText("تعديل الاسم", "Rename device"), fontSize = 12.sp, color = RichGreenButton)
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
                    usage == null -> appText("جارٍ قراءة الاستهلاك...", "Reading usage…")
                    !usage.isMeasured -> appText("عداد الاستهلاك غير متاح حالياً", "Usage counter is not available")
                    usage.quotaBytes <= 0L -> appText("الاستهلاك: ${formatUsageBytes(usage.bytesUsed)}", "Used: ${formatUsageBytes(usage.bytesUsed)}")
                    else -> appText("${formatUsageBytes(usage.bytesUsed)} من ${formatUsageBytes(usage.quotaBytes)}", "${formatUsageBytes(usage.bytesUsed)} of ${formatUsageBytes(usage.quotaBytes)}")
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
                text = appText("الكود: ${session.voucherCode}", "Voucher: ${session.voucherCode}"),
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
            title = { Text(appText("تعديل اسم الجهاز", "Rename device")) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text(appText("اسم الجهاز", "Device name")) },
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
                ) { Text(appText("حفظ", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appText("إلغاء", "Cancel")) }
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
    val fallbackName = appText("جهاز ${session.macAddress.takeLast(5)}", "Device ${session.macAddress.takeLast(5)}")
    val deviceName = usage?.displayName?.takeIf { it.isNotBlank() } ?: fallbackName
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
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(AppPalette.Mint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.SystemMonitor,
                        contentDescription = appText("جهاز متصل", "Connected device"),
                        tint = RichGreenButton,
                        modifier = Modifier.size(27.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = deviceName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "IP: ${session.ipAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "MAC: ${session.macAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = appText("الكرت المستخدم: ${session.voucherCode}", "Voucher used: ${session.voucherCode}"),
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = RichGreenButton,
                        textAlign = TextAlign.Start,
                        maxLines = 1
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
                            modifier = Modifier.size(70.dp),
                            color = usageColor,
                            trackColor = AppPalette.Mint,
                            strokeWidth = 6.dp
                        )
                        Text(
                            text = if (quotaBytes > 0L) "${(usageFraction * 100).toInt()}%" else "—",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeepGreenHeader
                        )
                    }
                    Text(
                        text = when {
                            usage == null -> appText("جارٍ القياس", "Measuring…")
                            quotaBytes > 0L -> "${formatUsageBytes(usedBytes)} / ${formatUsageBytes(quotaBytes)}"
                            usage.isMeasured -> formatUsageBytes(usedBytes)
                            else -> appText("غير متاح", "Not available")
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
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = appText("تعديل الاسم", "Rename device"),
                            modifier = Modifier.size(15.dp),
                            tint = RichGreenButton
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appText("الاسم", "Name"), fontSize = 11.sp, color = RichGreenButton)
                    }

                    OutlinedButton(
                        onClick = { showBlockDialog = true },
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                    ) {
                        Icon(
                            imageVector = AppIcons.Block,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(appText("حظر", "Block"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(appText("تعديل اسم الجهاز", "Rename device")) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text(appText("اسم الجهاز", "Device name")) },
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
                ) { Text(appText("حفظ", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appText("إلغاء", "Cancel")) }
            }
        )
    }

    if (showBlockDialog) {
        AlertDialog(
            onDismissRequest = { showBlockDialog = false },
            title = { Text(appText("حظر الجهاز؟", "Block this device?")) },
            text = {
                Text(appText("سيتم قطع الإنترنت عن الجهاز وحذف البطاقة المتصل بها. عند رفع الحظر سيحتاج إلى تسجيل الدخول ببطاقة جديدة.", "This will cut off the device's internet and delete its active voucher. After unblocking, it will need to sign in with a new voucher."))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showBlockDialog = false
                        onBlock()
                    }
                ) { Text(appText("حظر وحذف البطاقة", "Block and delete voucher"), color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showBlockDialog = false }) { Text(appText("إلغاء", "Cancel")) }
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
    voucherCode: String?,
    onRename: (String, String) -> Unit,
    onUnblock: () -> Unit
) {
    var showRenameDialog by remember(blockedMac.macAddress) { mutableStateOf(false) }
    var editedName by remember(blockedMac.macAddress) { mutableStateOf(displayName) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.SystemMonitor,
                        contentDescription = appText("جهاز محظور", "Blocked device"),
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(25.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "MAC: ${blockedMac.macAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    if (!voucherCode.isNullOrBlank()) {
                        Text(
                            text = appText("الكرت المستخدم: $voucherCode", "Voucher used: $voucherCode"),
                            modifier = Modifier.fillMaxWidth(),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = RichGreenButton,
                            textAlign = TextAlign.Start,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = blockedMac.reason,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 10.sp,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Start
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFFFCDD2), thickness = 0.5.dp)

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
                    Text(appText("محظور", "Blocked"), fontSize = 12.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
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
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = appText("تعديل الاسم", "Rename device"),
                            modifier = Modifier.size(15.dp),
                            tint = RichGreenButton
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appText("الاسم", "Name"), fontSize = 11.sp, color = RichGreenButton)
                    }
                    Button(
                        onClick = onUnblock,
                        colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
                        shape = RoundedCornerShape(18.dp),
                        contentPadding = PaddingValues(horizontal = 11.dp, vertical = 5.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Login,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appText("رفع الحظر", "Unblock"), fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(appText("تعديل اسم الجهاز", "Rename device")) },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it.take(40) },
                    label = { Text(appText("اسم الجهاز", "Device name")) },
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
                ) { Text(appText("حفظ", "Save")) }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) { Text(appText("إلغاء", "Cancel")) }
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
                Text(appText("إلغاء الحظر", "Unblock"), fontSize = 12.sp, color = Color.White)
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
