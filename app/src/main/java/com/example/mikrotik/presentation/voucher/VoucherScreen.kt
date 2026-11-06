package com.example.mikrotik.presentation.voucher

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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.data.db.entity.VoucherEntity
import com.example.mikrotik.presentation.ui.theme.AppIcons
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.*

private val DeepGreenHeader = Color(0xFF0F3822)
private val RichGreenButton = Color(0xFF00796B)
private val PageBackground = Color(0xFFE6F0EC)
private val SeamlessCardBg = Color(0xFFFFFFFF)
private val WatermarkLineColor = Color(0xFFC5E0D6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoucherScreen(
    viewModel: VoucherViewModel
) {
    val vouchers by viewModel.vouchers.collectAsState()
    val deleteError by viewModel.deleteError.collectAsState()
    val creationError by viewModel.creationError.collectAsState()

    var selectedDurationIndex by rememberSaveable { mutableIntStateOf(0) }
    var dataAmount by rememberSaveable { mutableStateOf("500") }
    var selectedDataUnit by rememberSaveable { mutableStateOf(VoucherDataUnit.MB) }

    val durationOptions = listOf("يوم واحد", "7 أيام", "30 يوماً")

    var durationExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PageBackground)
    ) {
        // Top Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepGreenHeader)
                .padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "‹",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "إنشاء بطاقة",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Icon(
                imageVector = AppIcons.CreditCard,
                contentDescription = "Card",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
        }

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
                                Text("⏱", fontSize = 10.sp)
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
                                Text("📦", fontSize = 10.sp)
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

            // Section Header: Created Vouchers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "البطاقات التي تم إنشاؤها",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepGreenHeader
                )
                Spacer(modifier = Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD5ECE9)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("📋", fontSize = 12.sp)
                }
            }

            if (vouchers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SeamlessCardBg),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        text = "لا توجد بطاقات منشأة حالياً. قم بإنشاء بطاقة جديدة بالأعلى.",
                        modifier = Modifier.fillMaxWidth().padding(24.dp),
                        color = Color.Gray,
                        fontSize = 13.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                vouchers.forEach { voucher ->
                    VoucherItemCard(
                        voucher = voucher,
                        deleteError = deleteError,
                        onDelete = { item, onComplete -> viewModel.deleteVoucher(item, onComplete) }
                    )
                }
            }
        }
    }
}

@Composable
fun VoucherItemCard(
    voucher: VoucherEntity,
    deleteError: String?,
    onDelete: (VoucherEntity, (Boolean) -> Unit) -> Unit
) {
    var showDeleteConfirmation by remember(voucher.code) { mutableStateOf(false) }
    val dataText = formatVoucherDataLimit(voucher.dataLimitBytes)
    val daysText = "${voucher.durationMinutes / (24 * 60)} يوم"

    val expiryTime = voucher.firstUsedAt?.let { it + voucher.durationMinutes * 60 * 1000 }
    val isExpired = expiryTime != null && System.currentTimeMillis() >= expiryTime
    val statusText = if (isExpired) "منتهية" else if (voucher.isUsed) "نشطة" else "غير مستخدمة"
    val statusBg = if (isExpired) Color(0xFFFFEBEE) else if (voucher.isUsed) Color(0xFFE8F5E9) else Color(0xFFF5F5F5)
    val statusColor = if (isExpired) Color(0xFFC62828) else if (voucher.isUsed) Color(0xFF2E7D32) else Color(0xFF757575)

    val dateFormat = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault())
    val expiryDateStr = expiryTime?.let { dateFormat.format(Date(it)) }

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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "حذف",
                modifier = Modifier.clickable { showDeleteConfirmation = true },
                fontSize = 12.sp,
                color = Color(0xFFC62828)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusBg)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Details
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$dataText - $daysText (${voucher.code})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = DeepGreenHeader
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = when {
                            expiryDateStr == null -> "تبدأ الصلاحية عند أول استخدام"
                            isExpired -> "انتهت في: $expiryDateStr"
                            else -> "تنتهي في: $expiryDateStr"
                        },
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            // Wi-Fi Icon Badge
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = AppIcons.WifiSignal,
                    contentDescription = "Wifi",
                    tint = RichGreenButton,
                    modifier = Modifier.size(22.dp)
                )
            }
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
