package com.example.mikrotik.presentation.dashboard

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.draw.rotate
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
import com.example.mikrotik.core.network.UsageSampleSource
import java.text.DateFormat
import java.util.Date
import java.util.Locale

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border
private fun String.canonicalMac(): String = trim().lowercase()

enum class DashboardTab {
    ACTIVE,
    BLOCKED
}

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

    var selectedTab by remember { mutableStateOf(DashboardTab.ACTIVE) }

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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Card (Compact & Professional)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, WatermarkLineColor, RoundedCornerShape(14.dp)),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${visibleActiveSessions.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = RichGreenButton
                        )
                        Text(
                            text = appText("الجلسات الفعالة", "Active sessions"),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(32.dp)
                            .background(WatermarkLineColor)
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${uniqueBlockedMacs.size}",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFC62828)
                        )
                        Text(
                            text = appText("الأجهزة المحظورة", "Blocked devices"),
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            // Two Filter Buttons (Active Devices vs Blocked Devices)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val tabs = listOf(
                    DashboardTab.ACTIVE to appText("الأجهزة النشطة", "Active devices") to visibleActiveSessions.size,
                    DashboardTab.BLOCKED to appText("الأجهزة المحظورة", "Blocked devices") to uniqueBlockedMacs.size
                )

                tabs.forEach { (pair, count) ->
                    val (tab, title) = pair
                    val isSelected = selectedTab == tab
                    Surface(
                        onClick = { selectedTab = tab },
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) RichGreenButton else SeamlessCardBg,
                        border = BorderStroke(1.dp, if (isSelected) RichGreenButton else WatermarkLineColor),
                        shadowElevation = if (isSelected) 2.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$title ($count)",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else DeepGreenHeader
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                DashboardTab.ACTIVE -> {
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
                                    .padding(18.dp),
                                color = Color.Gray,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        visibleActiveSessions.forEach { session ->
                            DeviceUsageCard(
                                session = session,
                                usage = deviceUsage[session.macAddress],
                                viewModel = viewModel,
                                onBlock = { viewModel.blockDevice(session.macAddress) },
                                onRename = viewModel::saveDeviceName
                            )
                        }
                    }
                }
                DashboardTab.BLOCKED -> {
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
                                    .padding(18.dp),
                                color = Color.Gray,
                                fontSize = 12.sp,
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
    viewModel: DashboardViewModel,
    onBlock: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val deviceName = viewModel.getDeviceName(session.macAddress, session.voucherCode)
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
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onBlock,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(appText("حظر", "Block"), fontSize = 11.sp, color = Color.White)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "IP: ${session.ipAddress}",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = DeepGreenHeader
                        )
                        Text(
                            text = "MAC: ${session.macAddress}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2F1)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = AppIcons.ConnectedDevices,
                            contentDescription = "Device",
                            tint = RichGreenButton,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = {
                        editedName = if (deviceName == session.voucherCode) "" else deviceName
                        showRenameDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                ) {
                    Text(appText("تعديل الاسم", "Rename"), fontSize = 11.sp, color = RichGreenButton)
                }
                Text(
                    text = deviceName,
                    fontSize = 14.sp,
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
                fontSize = 11.sp,
                color = Color.Gray,
                textAlign = TextAlign.End
            )
            LinearProgressIndicator(
                progress = usageFraction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = when {
                    usageFraction >= 0.9f -> Color(0xFFC62828)
                    usageFraction >= 0.7f -> Color(0xFFF9A825)
                    else -> RichGreenButton
                },
                trackColor = AppPalette.Mint
            )
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            // Professional Voucher Badge
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = AppPalette.Mint.copy(alpha = 0.5f),
                border = BorderStroke(0.5.dp, RichGreenButton.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.CreditCard,
                            contentDescription = null,
                            tint = RichGreenButton,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = session.voucherCode,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepGreenHeader
                        )
                    }
                    Text(
                        text = appText("الكرت المستخدم", "Voucher used"),
                        fontSize = 10.sp,
                        color = RichGreenButton,
                        fontWeight = FontWeight.SemiBold
                    )
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
}

@Composable
private fun DeviceUsageCard(
    session: ActiveSessionEntity,
    usage: DeviceUsageInfo?,
    viewModel: DashboardViewModel,
    onBlock: () -> Unit,
    onRename: (String, String) -> Unit
) {
    val deviceName = viewModel.getDeviceName(session.macAddress, session.voucherCode)
    val language = LocalAppLanguage.current
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
    val lastUpdate = usage?.lastSampleAtMillis?.let { formatSampleTimestamp(it, language) }
    val sourceValue = when (usage?.sampleSource ?: UsageSampleSource.UNAVAILABLE) {
        UsageSampleSource.WIFI_STATION -> appText("محطة Wi-Fi", "Wi-Fi station")
        UsageSampleSource.IPTABLES -> "iptables"
        UsageSampleSource.BOTH -> appText("كلاهما", "Both")
        UsageSampleSource.UNAVAILABLE -> appText("غير متاح", "Unavailable")
    }

    var showRenameDialog by remember(session.macAddress) { mutableStateOf(false) }
    var showBlockDialog by remember(session.macAddress) { mutableStateOf(false) }
    var showDetails by remember(session.macAddress) { mutableStateOf(false) }
    var editedName by remember(session.macAddress) { mutableStateOf("") }
    val usageLabel = when {
        usage == null -> appText("جارٍ قياس الاستهلاك...", "Measuring usage…")
        !usage.isMeasured -> appText("عداد الاستهلاك غير متاح", "Usage counter unavailable")
        quotaBytes > 0L -> "${formatUsageBytes(usedBytes)} / ${formatUsageBytes(quotaBytes)}"
        else -> formatUsageBytes(usedBytes)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, WatermarkLineColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = deviceName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = appText("استهلاك البيانات", "Data usage"),
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 10.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = usageLabel,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { usageFraction },
                        modifier = Modifier.size(44.dp),
                        color = usageColor,
                        trackColor = AppPalette.Mint,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = if (quotaBytes > 0L) "${(usageFraction * 100).toInt()}%" else "—",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        editedName = if (deviceName == session.voucherCode) "" else deviceName
                        showRenameDialog = true
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, RichGreenButton.copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RichGreenButton)
                ) {
                    Icon(
                        imageVector = AppIcons.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = RichGreenButton
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appText("إعادة التسمية", "Rename"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = RichGreenButton
                    )
                }

                OutlinedButton(
                    onClick = { showBlockDialog = true },
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFC62828).copy(alpha = 0.7f)),
                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFC62828))
                ) {
                    Icon(
                        imageVector = AppIcons.Block,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color(0xFFC62828)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = appText("حظر الجهاز", "Block device"),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFC62828)
                    )
                }
            }

            HorizontalDivider(color = WatermarkLineColor, thickness = 0.5.dp)

            TextButton(
                onClick = { showDetails = !showDetails },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
            ) {
                Text(
                    text = if (showDetails) appText("إخفاء التفاصيل", "Hide details")
                    else appText("عرض التفاصيل", "Show details"),
                    modifier = Modifier.weight(1f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RichGreenButton,
                    textAlign = TextAlign.Start
                )
                Icon(
                    imageVector = AppIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(if (showDetails) 270f else 90f),
                    tint = RichGreenButton
                )
            }

            if (showDetails) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(AppPalette.Mint.copy(alpha = 0.35f))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    DeviceDetailRow(
                        label = appText("عنوان IP", "IP address"),
                        value = session.ipAddress,
                        monospace = true
                    )
                    DeviceDetailRow(
                        label = appText("عنوان MAC", "MAC address"),
                        value = session.macAddress,
                        monospace = true
                    )
                    DeviceDetailRow(
                        label = appText("الكرت المستخدم", "Voucher used"),
                        value = session.voucherCode,
                        monospace = true
                    )
                    DeviceDetailRow(
                        label = appText("الاستهلاك", "Usage"),
                        value = usageLabel
                    )
                    DeviceDetailRow(
                        label = appText("آخر تحديث", "Last updated"),
                        value = lastUpdate?.let { it } ?: appText("غير متاح", "Not available")
                    )
                    DeviceDetailRow(
                        label = appText("مصدر القراءة", "Reading source"),
                        value = sourceValue
                    )
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

@Composable
private fun DeviceDetailRow(
    label: String,
    value: String,
    monospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(0.42f),
            fontSize = 11.sp,
            color = AppPalette.Muted
        )
        Text(
            text = value,
            modifier = Modifier.weight(0.58f),
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = DeepGreenHeader,
            textAlign = TextAlign.End
        )
    }
}

private fun formatUsageBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024 * 1024))} GB"
    bytes >= 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024))} MB"
    bytes >= 1024L -> "${"%.1f".format(bytes / 1024.0)} KB"
    else -> "$bytes B"
}

private fun formatSampleTimestamp(timestampMillis: Long, language: AppLanguage): String {
    val locale = if (language == AppLanguage.ARABIC) Locale("ar") else Locale.ENGLISH
    return DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, locale)
        .format(Date(timestampMillis))
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
            .border(1.dp, Color(0xFFFFCDD2), RoundedCornerShape(14.dp)),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.SystemMonitor,
                        contentDescription = appText("جهاز محظور", "Blocked device"),
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = displayName,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DeepGreenHeader,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "MAC: ${blockedMac.macAddress}",
                        modifier = Modifier.fillMaxWidth(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = AppPalette.Muted,
                        textAlign = TextAlign.Start
                    )
                    Text(
                        text = blockedMac.reason,
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 10.sp,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Start
                    )
                }
            }

            if (!voucherCode.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFEBEE).copy(alpha = 0.6f),
                    border = BorderStroke(0.5.dp, Color(0xFFEF9A9A))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = AppIcons.CreditCard,
                                contentDescription = null,
                                tint = Color(0xFFC62828),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = voucherCode,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenHeader
                            )
                        }
                        Text(
                            text = appText("الكرت المحظور", "Blocked voucher"),
                            fontSize = 10.sp,
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.SemiBold
                        )
                    }
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
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFC62828))
                    )
                    Text(appText("محظور", "Blocked"), fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.Bold)
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            editedName = displayName
                            showRenameDialog = true
                        },
                        modifier = Modifier.height(34.dp),
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, RichGreenButton.copy(alpha = 0.7f)),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = RichGreenButton)
                    ) {
                        Icon(
                            imageVector = AppIcons.Edit,
                            contentDescription = appText("تعديل الاسم", "Rename"),
                            modifier = Modifier.size(13.dp),
                            tint = RichGreenButton
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(appText("إعادة التسمية", "Rename"), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = RichGreenButton)
                    }
                    Button(
                        onClick = onUnblock,
                        modifier = Modifier.height(34.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = AppIcons.Login,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
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
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Button(
                onClick = onUnblock,
                colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                modifier = Modifier.height(30.dp)
            ) {
                Text(appText("إلغاء الحظر", "Unblock"), fontSize = 11.sp, color = Color.White)
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "MAC: ${blockedMac.macAddress}",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
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
