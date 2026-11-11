package com.example.mikrotik.presentation.voucher

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.mikrotik.data.db.entity.VoucherEntity
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

private val DeepGreenHeader get() = AppPalette.TextNavy
private val RichGreenButton get() = AppPalette.Teal
private val SeamlessCardBg get() = AppPalette.Card
private val WatermarkLineColor get() = AppPalette.Border

enum class VoucherFilterTab {
    UNUSED,
    ACTIVE,
    EXPIRED
}

@Composable
private fun VoucherFilterTab.localizedTitle(): String = when (this) {
    VoucherFilterTab.UNUSED -> appText("غير مستخدمة", "Unused")
    VoucherFilterTab.ACTIVE -> appText("نشطة", "Active")
    VoucherFilterTab.EXPIRED -> appText("منتهية", "Expired")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherScreen(
    viewModel: VoucherViewModel
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val activeVoucherNames by viewModel.activeVoucherNames.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val creationError by viewModel.creationError.collectAsState()
    val creationSuccess by viewModel.creationSuccess.collectAsState()
    val context = LocalContext.current

    var selectedFilterTab by remember { mutableStateOf(VoucherFilterTab.UNUSED) }
    var durationDays by rememberSaveable { mutableStateOf("2") }
    var packageCapacityAmount by rememberSaveable { mutableStateOf("") }
    var dataAmount by rememberSaveable { mutableStateOf("") }
    var selectedDataUnit by rememberSaveable { mutableStateOf(VoucherDataUnit.MB) }
    val savedPackageCapacityBytes by viewModel.packageCapacityBytes.collectAsState()
    val packageBudget by viewModel.packageBudget.collectAsState()
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        if (packageCapacityAmount.isBlank() && savedPackageCapacityBytes > 0L) {
            packageCapacityAmount = formatGigabytesInput(savedPackageCapacityBytes)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            currentTime = System.currentTimeMillis()
        }
    }

    val unusedVouchers = vouchers.filter { !it.isUsed && !it.isExpiredAt(currentTime) }
    val activeVouchers = vouchers.filter { it.isUsed && !it.isExpiredAt(currentTime) }
    val expiredVouchers = vouchers.filter { it.isExpiredAt(currentTime) }

    val copiedToastText = appText("تم نسخ رمز البطاقة", "Voucher code copied")
    val copyVoucherCode: (String) -> Unit = { code ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Voucher code", code))
        Toast.makeText(context, copiedToastText, Toast.LENGTH_SHORT).show()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(
            title = appText("إنشاء بطاقة", "Create voucher"),
            subtitle = "AL RABDI NET",
            startIcon = AppIcons.CreditCard
        )

        // Scrollable Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // Creation Card
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
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Total package size comes first and is persisted as the user edits it.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = AppIcons.Inventory,
                                contentDescription = appText("حجم الباقة الإجمالي", "Total package size"),
                                tint = RichGreenButton,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = appText("حجم الباقة الإجمالي", "Total package size"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenHeader
                            )
                        }
                        OutlinedTextField(
                            value = packageCapacityAmount,
                            onValueChange = { value ->
                                packageCapacityAmount = value
                                viewModel.updatePackageCapacity(value)
                                viewModel.clearCreationError()
                            },
                            label = { Text(appText("أدخل سعة الباقة", "Enter package capacity")) },
                            placeholder = { Text(appText("مثال: 5 أو 0.5", "e.g. 5 or 0.5")) },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                Text(
                                    text = "GB",
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = RichGreenButton,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RichGreenButton,
                                unfocusedBorderColor = WatermarkLineColor,
                                focusedContainerColor = AppPalette.Card,
                                unfocusedContainerColor = AppPalette.Card
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    val enteredCapacityBytes = VoucherSizeParser.toBytes(packageCapacityAmount, VoucherDataUnit.GB)
                    val previewCapacityBytes = enteredCapacityBytes ?: packageBudget.capacityBytes
                    val previewRemainingBytes = (previewCapacityBytes - packageBudget.committedBytes).coerceAtLeast(0L)
                    Text(
                        text = if (previewCapacityBytes > 0L) {
                            appText(
                                "المخصص للبطاقات: ${formatVoucherDataLimit(packageBudget.committedBytes)}  •  المتاح: ${formatVoucherDataLimit(previewRemainingBytes)}",
                                "Reserved for vouchers: ${formatVoucherDataLimit(packageBudget.committedBytes)}  •  Available: ${formatVoucherDataLimit(previewRemainingBytes)}"
                            )
                        } else {
                            appText("أدخل حجم الباقة لعرض المساحة المتاحة للبطاقات.", "Enter the package size to see data available for vouchers.")
                        },
                        fontSize = 12.sp,
                        color = AppPalette.Muted
                    )
                    if (previewCapacityBytes > 0L && packageBudget.committedBytes > previewCapacityBytes) {
                        Text(
                            text = appText(
                                "البطاقات الحالية تجاوزت سعة الباقة؛ يمكنك تعديل الرقم، لكن لن تُنشأ بطاقة جديدة حتى يصبح هناك رصيد متاح.",
                                "Existing vouchers exceed this package size. You can change the value, but a new voucher requires available capacity."
                            ),
                            fontSize = 12.sp,
                            color = Color(0xFFC62828)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Inventory,
                                    contentDescription = appText("حجم الاستهلاك", "Data limit"),
                                    tint = RichGreenButton,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = appText("حجم الاستهلاك", "Data limit"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenHeader
                                )
                            }
                            OutlinedTextField(
                                value = dataAmount,
                                onValueChange = { value ->
                                    if (value.length <= 20) dataAmount = value
                                    viewModel.clearCreationError()
                                },
                                label = { Text(appText("الحجم", "Amount")) },
                                placeholder = { Text(appText("مثال: 500", "e.g. 500")) },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text(
                                        text = selectedDataUnit.label,
                                        modifier = Modifier.padding(end = 6.dp),
                                        color = RichGreenButton,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RichGreenButton,
                                    unfocusedBorderColor = WatermarkLineColor,
                                    focusedContainerColor = AppPalette.Card,
                                    unfocusedContainerColor = AppPalette.Card
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = AppIcons.Clock,
                                    contentDescription = appText("مدة الصلاحية", "Duration"),
                                    tint = RichGreenButton,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = appText("مدة الأيام", "Validity days"),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DeepGreenHeader
                                )
                            }
                            OutlinedTextField(
                                value = durationDays,
                                onValueChange = { value ->
                                    if (value.length <= 7 && value.all(Char::isDigit)) durationDays = value
                                    viewModel.clearCreationError()
                                },
                                label = { Text(appText("بالأيام", "Days")) },
                                placeholder = { Text(appText("مثال: 2", "e.g. 2")) },
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                trailingIcon = {
                                    Text(
                                        text = appText("يوم", "days"),
                                        modifier = Modifier.padding(end = 6.dp),
                                        color = RichGreenButton,
                                        fontWeight = FontWeight.Bold
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RichGreenButton,
                                    unfocusedBorderColor = WatermarkLineColor,
                                    focusedContainerColor = AppPalette.Card,
                                    unfocusedContainerColor = AppPalette.Card
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        VoucherDataUnit.entries.forEach { unit ->
                            FilterChip(
                                selected = selectedDataUnit == unit,
                                onClick = {
                                    selectedDataUnit = unit
                                    viewModel.clearCreationError()
                                },
                                label = {
                                    Text(
                                        if (unit == VoucherDataUnit.MB) appText("ميغابايت (MB)", "Megabytes (MB)")
                                        else appText("غيغابايت (GB)", "Gigabytes (GB)")
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AppPalette.MintStrong,
                                    selectedLabelColor = DeepGreenHeader
                                )
                            )
                        }
                    }
                    Text(
                        text = appText("يمكن إدخال كسور مثل 1.5، ويُحسب 1 GB = 1024 MB.", "Decimal amounts are supported (e.g. 1.5); 1 GB = 1024 MB."),
                        fontSize = 12.sp,
                        color = AppPalette.Muted
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Create Button
                    Button(
                        onClick = {
                            val days = durationDays.toIntOrNull() ?: 0
                            viewModel.createVoucher(days, dataAmount, selectedDataUnit, packageCapacityAmount)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RichGreenButton)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = AppIcons.CreditCard,
                                contentDescription = "Card",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = appText("أنشئ بطاقة", "Create voucher"),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    creationError?.let { error ->
                        Text(
                            text = localizeVoucherError(error, deleting = false),
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Filter Tabs (3 buttons: غير مستخدمة، نشطة، منتهية)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val filterTabs = listOf(
                    VoucherFilterTab.UNUSED to unusedVouchers.size,
                    VoucherFilterTab.ACTIVE to activeVouchers.size,
                    VoucherFilterTab.EXPIRED to expiredVouchers.size
                )

                filterTabs.forEach { (tab, count) ->
                    val isSelected = selectedFilterTab == tab
                    Surface(
                        onClick = { selectedFilterTab = tab },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
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
                                text = "${tab.localizedTitle()} ($count)",
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                                color = if (isSelected) Color.White else DeepGreenHeader
                            )
                        }
                    }
                }
            }

            when (selectedFilterTab) {
                VoucherFilterTab.UNUSED -> VoucherStatusSection(
                    title = appText("بطاقات غير مستخدمة", "Unused vouchers"),
                    vouchers = unusedVouchers,
                    emptyMessage = appText("لا توجد بطاقات غير مستخدمة حالياً.", "No unused vouchers." ),
                    statusTint = if (AppPalette.isDarkMode) Color(0xFFC2CDD2) else Color(0xFF757575),
                    statusBackground = if (AppPalette.isDarkMode) Color(0xFF303E44) else Color(0xFFF1F3F4),
                    activeVoucherNames = activeVoucherNames,
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
                VoucherFilterTab.ACTIVE -> VoucherStatusSection(
                    title = appText("بطاقات نشطة", "Active vouchers"),
                    vouchers = activeVouchers,
                    emptyMessage = appText("لا توجد بطاقات نشطة حالياً.", "No active vouchers." ),
                    statusTint = if (AppPalette.isDarkMode) Color(0xFF83D8A0) else Color(0xFF2E7D32),
                    statusBackground = if (AppPalette.isDarkMode) Color(0xFF203D2B) else Color(0xFFE8F5E9),
                    activeVoucherNames = activeVoucherNames,
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
                VoucherFilterTab.EXPIRED -> VoucherStatusSection(
                    title = appText("بطاقات منتهية", "Expired vouchers"),
                    vouchers = expiredVouchers,
                    emptyMessage = appText("لا توجد بطاقات منتهية حالياً.", "No expired vouchers." ),
                    statusTint = if (AppPalette.isDarkMode) Color(0xFFFF8A80) else Color(0xFFC62828),
                    statusBackground = if (AppPalette.isDarkMode) Color(0xFF492727) else Color(0xFFFFEBEE),
                    activeVoucherNames = activeVoucherNames,
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
            }
        }
    }

    val dismissCreationAndReset = {
        durationDays = ""
        dataAmount = ""
        viewModel.dismissCreationSuccess()
    }

    creationSuccess?.let { code ->
        AlertDialog(
            onDismissRequest = dismissCreationAndReset,
            icon = {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = appText("تم بنجاح", "Success"),
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = appText("تم إنشاء البطاقة بنجاح", "Voucher created successfully"),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = appText("رمز البطاقة: $code", "Voucher code: $code"),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = dismissCreationAndReset) {
                    Text(appText("موافق", "OK"), color = RichGreenButton, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
private fun VoucherStatusSection(
    title: String,
    vouchers: List<VoucherEntity>,
    emptyMessage: String,
    statusTint: Color,
    statusBackground: Color,
    activeVoucherNames: Map<String, String>,
    deleteError: String?,
    onCopy: (String) -> Unit,
    onDelete: (VoucherEntity, (Boolean) -> Unit) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = AppIcons.Inventory,
                contentDescription = null,
                tint = statusTint,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(statusBackground)
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = vouchers.size.toString(),
                    color = statusTint,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (vouchers.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = emptyMessage,
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    color = AppPalette.Muted,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            vouchers.forEach { voucher ->
                VoucherItemCard(
                    voucher = voucher,
                    statusTitle = title,
                    statusTint = statusTint,
                    statusBackground = statusBackground,
                    activeDeviceName = activeVoucherNames[voucher.code],
                    deleteError = deleteError,
                    onCopy = onCopy,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun VoucherItemCard(
    voucher: VoucherEntity,
    statusTitle: String,
    statusTint: Color,
    statusBackground: Color,
    activeDeviceName: String?,
    deleteError: String?,
    onCopy: (String) -> Unit,
    onDelete: (VoucherEntity, (Boolean) -> Unit) -> Unit
) {
    var showDeleteConfirmation by remember(voucher.code) { mutableStateOf(false) }
    val dataText = formatVoucherDataLimit(voucher.dataLimitBytes)
    val dayCount = voucher.durationMinutes / (24 * 60)
    val daysText = appText("$dayCount يوم", "$dayCount days")
    val expiryTime = voucher.expiryTime()
    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val expiryDateStr = expiryTime?.let { dateFormat.format(Date(it)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, statusTint.copy(alpha = 0.24f), RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBackground)
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = statusTitle,
                        color = statusTint,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                IconButton(onClick = { onCopy(voucher.code) }) {
                    Icon(
                        imageVector = AppIcons.ContentCopy,
                        contentDescription = appText("نسخ رمز البطاقة", "Copy voucher code"),
                        tint = RichGreenButton
                    )
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = appText("حذف البطاقة", "Delete voucher"),
                        tint = Color(0xFFC62828)
                    )
                }
            }

            Text(
                text = voucher.code,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = FontFamily.Monospace,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = DeepGreenHeader,
                textAlign = TextAlign.Start
            )

            if (!activeDeviceName.isNullOrBlank()) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = AppPalette.Mint.copy(alpha = 0.5f),
                    border = BorderStroke(0.5.dp, RichGreenButton.copy(alpha = 0.3f))
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
                                imageVector = AppIcons.ConnectedDevices,
                                contentDescription = null,
                                tint = RichGreenButton,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = activeDeviceName,
                                fontFamily = FontFamily.Default,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenHeader
                            )
                        }
                        Text(
                            text = appText("المتصل (التسمية)", "Connected (Label)"),
                            fontSize = 10.sp,
                            color = RichGreenButton,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            HorizontalDivider(color = WatermarkLineColor, thickness = 0.7.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = appText("السعة: $dataText", "Data: $dataText"), fontSize = 13.sp, color = DeepGreenHeader)
                Text(text = appText("الصلاحية: $daysText", "Duration: $daysText"), fontSize = 13.sp, color = DeepGreenHeader)
            }
            Text(
                text = when {
                    expiryDateStr == null -> appText("تبدأ الصلاحية عند أول استخدام", "Validity starts on first use")
                    statusTitle == appText("بطاقات منتهية", "Expired vouchers") -> appText("انتهت الصلاحية: $expiryDateStr", "Expired: $expiryDateStr")
                    else -> appText("تنتهي الصلاحية: $expiryDateStr", "Expires: $expiryDateStr")
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = AppPalette.Muted,
                textAlign = TextAlign.Start
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text(appText("حذف البطاقة؟", "Delete voucher?")) },
            text = {
                Text(
                    deleteError?.let { localizeVoucherError(it, deleting = true) } ?: appText("سيُحذف الرمز نهائيًا، ويُسحب الإنترنت فورًا من الجهاز المرتبط به. لن يمكن استخدام البطاقة بعد حذفها.", "This voucher will be permanently deleted and internet access revoked immediately. It cannot be used again.")
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(voucher) { succeeded ->
                            if (succeeded) showDeleteConfirmation = false
                        }
                    }
                ) { Text(appText("حذف البطاقة", "Delete voucher"), color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text(appText("إلغاء", "Cancel")) }
            }
        )
    }
}


@Composable
private fun localizeVoucherError(message: String, deleting: Boolean): String = when {
    message.contains("غير كافية") -> appText(
        "عذراً، باقتك غير كافية لإنشاء هذه البطاقة.",
        "Sorry, your package does not have enough data for this voucher."
    )
    message.contains("حجم الباقة") -> appText(
        "أدخل حجم الباقة الإجمالي بوحدة GB.",
        "Enter the total package size in GB."
    )
    message.contains("السعة المستخدمة أو المحجوزة") -> appText(
        "حجم الباقة أقل من السعة المستخدمة أو المحجوزة حالياً.",
        "Package size is below the data already used or reserved."
    )
    message.contains("الأيام") -> appText(
        "أدخل عدداً صحيحاً من الأيام أكبر من صفر.",
        "Enter a whole number of days greater than zero."
    )
    message.contains("حجم") || message.contains("صحيح") -> appText(
        "أدخل حجماً صحيحاً أكبر من صفر واختر MB أو GB.",
        "Enter a valid amount greater than zero and choose MB or GB."
    )
    deleting -> appText(message, "Could not delete the voucher or revoke access. Check root access and hotspot status, then retry.")
    else -> appText(message, "Could not create the voucher. Check the entered values and try again.")
}

private fun formatVoucherDataLimit(bytes: Long): String {
    val gigabyte = 1024L * 1024L * 1024L
    val megabyte = 1024L * 1024L
    val (divisor, unit) = if (bytes >= gigabyte) gigabyte to "GB" else megabyte to "MB"
    val amount = BigDecimal.valueOf(bytes)
        .divide(BigDecimal.valueOf(divisor), 2, RoundingMode.HALF_UP)
        .stripTrailingZeros()
        .toPlainString()
    return "$amount $unit"
}

private fun formatGigabytesInput(bytes: Long): String = BigDecimal.valueOf(bytes)
    .divide(BigDecimal.valueOf(1024L * 1024L * 1024L), 3, RoundingMode.HALF_UP)
    .stripTrailingZeros()
    .toPlainString()

private fun VoucherEntity.expiryTime(): Long? =
    firstUsedAt?.let { it + durationMinutes * 60_000L }

private fun VoucherEntity.isExpiredAt(now: Long): Boolean =
    bytesUsedSoFar >= dataLimitBytes || (expiryTime()?.let { now >= it } ?: false)
