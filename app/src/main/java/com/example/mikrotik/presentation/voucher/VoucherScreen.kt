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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.mikrotik.data.db.entity.VoucherEntity
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette
import kotlinx.coroutines.delay
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

private val DeepGreenHeader = AppPalette.TextNavy
private val RichGreenButton = AppPalette.Teal
private val SeamlessCardBg = AppPalette.Card
private val WatermarkLineColor = AppPalette.Border

enum class VoucherFilterTab(val title: String) {
    UNUSED("غير مستخدمة"),
    ACTIVE("نشطة"),
    EXPIRED("منتهية")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherScreen(
    viewModel: VoucherViewModel
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val creationError by viewModel.creationError.collectAsState()
    val creationSuccess by viewModel.creationSuccess.collectAsState()
    val context = LocalContext.current

    var selectedFilterTab by remember { mutableStateOf(VoucherFilterTab.UNUSED) }
    var selectedDurationIndex by rememberSaveable { mutableIntStateOf(0) }
    var dataAmount by rememberSaveable { mutableStateOf("500") }
    var selectedDataUnit by rememberSaveable { mutableStateOf(VoucherDataUnit.MB) }
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            currentTime = System.currentTimeMillis()
        }
    }

    val unusedVouchers = vouchers.filter { !it.isUsed && !it.isExpiredAt(currentTime) }
    val activeVouchers = vouchers.filter { it.isUsed && !it.isExpiredAt(currentTime) }
    val expiredVouchers = vouchers.filter { it.isExpiredAt(currentTime) }

    val copyVoucherCode: (String) -> Unit = { code ->
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Voucher code", code))
        Toast.makeText(context, "تم نسخ رمز البطاقة", Toast.LENGTH_SHORT).show()
    }

    val durationOptions = listOf("يوم واحد", "7 أيام", "30 يوماً")

    var durationExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        AppTopBar(
            title = "إنشاء بطاقة",
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
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
                    // Duration Dropdown
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "مدة صلاحية البطاقة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenHeader
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Small icon placeholder
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Clock,
                                    contentDescription = "مدة الصلاحية",
                                    tint = RichGreenButton,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        ExposedDropdownMenuBox(
                            expanded = durationExpanded,
                            onExpandedChange = { durationExpanded = !durationExpanded }
                        ) {
                            OutlinedTextField(
                                value = durationOptions[selectedDurationIndex],
                                onValueChange = {},
                                readOnly = true,
                                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = durationExpanded) },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = RichGreenButton,
                                    unfocusedBorderColor = WatermarkLineColor,
                                    focusedContainerColor = Color(0xFFFAFCFB),
                                    unfocusedContainerColor = Color(0xFFFAFCFB)
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = durationExpanded,
                                onDismissRequest = { durationExpanded = false }
                            ) {
                                durationOptions.forEachIndexed { index, option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            selectedDurationIndex = index
                                            durationExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Custom data limit with an explicit unit.
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "حد استهلاك البطاقة",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepGreenHeader
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFE8F5E9)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = AppIcons.Inventory,
                                    contentDescription = "حد استهلاك البطاقة",
                                    tint = RichGreenButton,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        OutlinedTextField(
                            value = dataAmount,
                            onValueChange = { value ->
                                if (value.length <= 20) dataAmount = value
                                viewModel.clearCreationError()
                            },
                            label = { Text("أدخل الحجم") },
                            placeholder = { Text("مثال: 500 أو 1.5") },
                            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Right),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            trailingIcon = {
                                Text(
                                    text = selectedDataUnit.label,
                                    modifier = Modifier.padding(end = 8.dp),
                                    color = RichGreenButton,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = RichGreenButton,
                                unfocusedBorderColor = WatermarkLineColor,
                                focusedContainerColor = Color(0xFFFAFCFB),
                                unfocusedContainerColor = Color(0xFFFAFCFB)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

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
                                    label = { Text(if (unit == VoucherDataUnit.MB) "ميغابايت (MB)" else "غيغابايت (GB)") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(0xFFD5ECE9),
                                        selectedLabelColor = DeepGreenHeader
                                    )
                                )
                            }
                        }
                        Text(
                            text = "يمكن إدخال كسور مثل 1.5، ويُحسب 1 GB = 1024 MB.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Create Button
                    Button(
                        onClick = {
                            val days = when (selectedDurationIndex) {
                                0 -> 1
                                1 -> 7
                                else -> 30
                            }
                            viewModel.createVoucher(days, dataAmount, selectedDataUnit)
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
                                text = "أنشئ بطاقة",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                    creationError?.let { error ->
                        Text(
                            text = error,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFC62828),
                            fontSize = 13.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
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
                                text = "${tab.title} ($count)",
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
                    title = "بطاقات غير مستخدمة",
                    vouchers = unusedVouchers,
                    emptyMessage = "لا توجد بطاقات غير مستخدمة حالياً.",
                    statusTint = Color(0xFF757575),
                    statusBackground = Color(0xFFF1F3F4),
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
                VoucherFilterTab.ACTIVE -> VoucherStatusSection(
                    title = "بطاقات نشطة",
                    vouchers = activeVouchers,
                    emptyMessage = "لا توجد بطاقات نشطة حالياً.",
                    statusTint = Color(0xFF2E7D32),
                    statusBackground = Color(0xFFE8F5E9),
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
                VoucherFilterTab.EXPIRED -> VoucherStatusSection(
                    title = "بطاقات منتهية",
                    vouchers = expiredVouchers,
                    emptyMessage = "لا توجد بطاقات منتهية حالياً.",
                    statusTint = Color(0xFFC62828),
                    statusBackground = Color(0xFFFFEBEE),
                    deleteError = deleteError,
                    onCopy = copyVoucherCode,
                    onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                )
            }
        }
    }

    creationSuccess?.let { code ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissCreationSuccess() },
            icon = {
                Icon(
                    imageVector = AppIcons.Shield,
                    contentDescription = "تم بنجاح",
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = {
                Text(
                    text = "تم إنشاء البطاقة بنجاح",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "رمز البطاقة: $code",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissCreationSuccess() }) {
                    Text("موافق", color = RichGreenButton, fontWeight = FontWeight.Bold)
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
                    color = Color.Gray,
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
    deleteError: String?,
    onCopy: (String) -> Unit,
    onDelete: (VoucherEntity, (Boolean) -> Unit) -> Unit
) {
    var showDeleteConfirmation by remember(voucher.code) { mutableStateOf(false) }
    val dataText = formatVoucherDataLimit(voucher.dataLimitBytes)
    val daysText = "${voucher.durationMinutes / (24 * 60)} يوم"
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
                        contentDescription = "نسخ رمز البطاقة",
                        tint = RichGreenButton
                    )
                }
                IconButton(onClick = { showDeleteConfirmation = true }) {
                    Icon(
                        imageVector = AppIcons.Delete,
                        contentDescription = "حذف البطاقة",
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
            HorizontalDivider(color = WatermarkLineColor, thickness = 0.7.dp)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "السعة: $dataText", fontSize = 13.sp, color = DeepGreenHeader)
                Text(text = "الصلاحية: $daysText", fontSize = 13.sp, color = DeepGreenHeader)
            }
            Text(
                text = when {
                    expiryDateStr == null -> "تبدأ الصلاحية عند أول استخدام"
                    statusTitle == "منتهية" -> "انتهت الصلاحية: $expiryDateStr"
                    else -> "تنتهي الصلاحية: $expiryDateStr"
                },
                modifier = Modifier.fillMaxWidth(),
                fontSize = 12.sp,
                color = Color.Gray,
                textAlign = TextAlign.Start
            )
        }
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("حذف البطاقة؟") },
            text = {
                Text(
                    deleteError ?: "سيُحذف الرمز نهائيًا، ويُسحب الإنترنت فورًا من الجهاز المرتبط به. لن يمكن استخدام البطاقة بعد حذفها."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(voucher) { succeeded ->
                            if (succeeded) showDeleteConfirmation = false
                        }
                    }
                ) { Text("حذف البطاقة", color = Color(0xFFC62828)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) { Text("إلغاء") }
            }
        )
    }
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

private fun VoucherEntity.expiryTime(): Long? =
    firstUsedAt?.let { it + durationMinutes * 60_000L }

private fun VoucherEntity.isExpiredAt(now: Long): Boolean =
    bytesUsedSoFar >= dataLimitBytes || (expiryTime()?.let { now >= it } ?: false)
