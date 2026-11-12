package com.example.mikrotik.presentation.voucher

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.data.db.entity.VoucherEntity
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun VoucherPackageSettingsScreen(
    initialEnabled: Boolean,
    initialPackageType: String,
    vouchers: List<VoucherEntity>,
    capacityForType: (String) -> Long,
    unitForType: (String) -> VoucherDataUnit,
    budgetForType: (String, Long) -> VoucherPackageBudget,
    onBack: () -> Unit,
    onSave: (Boolean, String, String, VoucherDataUnit) -> Boolean
) {
    var enabled by rememberSaveable { mutableStateOf(initialEnabled) }
    var packageType by rememberSaveable { mutableStateOf(initialPackageType) }
    var capacityAmount by rememberSaveable(packageType) {
        mutableStateOf(capacityForType(packageType).takeIf { it > 0L }?.let {
            formatPackageCapacityInput(it, unitForType(packageType))
        } ?: "")
    }
    var capacityUnit by rememberSaveable(packageType) { mutableStateOf(unitForType(packageType)) }
    var errorText by rememberSaveable { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = System.currentTimeMillis()
        }
    }

    val normalizedType = VoucherPackageType.normalize(packageType)
    val budget = remember(packageType, vouchers, now) {
        normalizedType?.let { budgetForType(it, now) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = appText("حجم الباقة", "Package capacity"),
            subtitle = "AL RABDI NET",
            startIcon = AppIcons.ChevronRight,
            endIcon = null,
            onStartClick = onBack
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
                border = BorderStroke(1.dp, AppPalette.Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = appText("تفعيل حد حجم الباقة", "Enable package data limit"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppPalette.TextNavy
                            )
                            Text(
                                text = appText(
                                    "عند إيقافه تُنشأ البطاقات بلا فئة أو رصيد إجمالي محدد.",
                                    "When off, vouchers are created without a category or package-wide quota."
                                ),
                                fontSize = 12.sp,
                                color = AppPalette.Muted
                            )
                        }
                        Switch(checked = enabled, onCheckedChange = {
                            enabled = it
                            errorText = null
                        })
                    }

                    if (enabled) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = appText("الفئة", "Category"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.TextNavy
                            )
                            OutlinedTextField(
                                value = packageType,
                                onValueChange = { value ->
                                    if (value.length <= 12 && value.all {
                                            it.isLetterOrDigit() || it == '_' || it == '-'
                                        }
                                    ) {
                                        packageType = value.uppercase(Locale.ROOT)
                                    }
                                    errorText = null
                                },
                                label = { Text(appText("معرف الفئة", "Category ID")) },
                                placeholder = { Text("A") },
                                singleLine = true,
                                textStyle = TextStyle(textAlign = TextAlign.Start),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppPalette.Teal,
                                    unfocusedBorderColor = AppPalette.Border,
                                    focusedContainerColor = AppPalette.Card,
                                    unfocusedContainerColor = AppPalette.Card
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = appText(
                                    "يمكن استخدام أحرف أو أرقام حتى 12 خانة. لكل فئة رصيد مستقل.",
                                    "Use up to 12 letters or digits. Each category has its own quota."
                                ),
                                fontSize = 11.sp,
                                color = AppPalette.Muted
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = appText("حجم الباقة", "Package size"),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AppPalette.TextNavy
                            )
                            OutlinedTextField(
                                value = capacityAmount,
                                onValueChange = {
                                    capacityAmount = it
                                    errorText = null
                                },
                                label = { Text(appText("الحجم الإجمالي", "Total capacity")) },
                                placeholder = { Text(appText("مثال: 100", "e.g. 100")) },
                                singleLine = true,
                                textStyle = TextStyle(textAlign = TextAlign.Start),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                trailingIcon = {
                                    Text(
                                        text = capacityUnit.label,
                                        modifier = Modifier.padding(end = 8.dp),
                                        fontWeight = FontWeight.Bold,
                                        color = AppPalette.Teal
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AppPalette.Teal,
                                    unfocusedBorderColor = AppPalette.Border,
                                    focusedContainerColor = AppPalette.Card,
                                    unfocusedContainerColor = AppPalette.Card
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                VoucherDataUnit.entries.forEach { unit ->
                                    FilterChip(
                                        selected = capacityUnit == unit,
                                        onClick = {
                                            if (capacityUnit != unit) {
                                                val currentBytes = VoucherSizeParser.toBytes(capacityAmount, capacityUnit)
                                                    ?: capacityForType(packageType).takeIf { capacityAmount.isBlank() }
                                                capacityUnit = unit
                                                capacityAmount = currentBytes?.let {
                                                    formatPackageCapacityInput(it, unit)
                                                } ?: capacityAmount
                                            }
                                        },
                                        label = { Text(unit.label) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = AppPalette.MintStrong,
                                            selectedLabelColor = AppPalette.TextNavy
                                        )
                                    )
                                }
                            }
                        }

                        if (normalizedType != null && budget != null) {
                            val enteredBytes = VoucherSizeParser.toBytes(capacityAmount, capacityUnit)
                                ?: budget.capacityBytes
                            val remaining = (enteredBytes - budget.committedBytes).coerceAtLeast(0L)
                            Text(
                                text = appText(
                                    "الفئة $normalizedType  •  المحجوز: ${formatVoucherDataLimit(budget.committedBytes)}  •  المتبقي: ${formatVoucherDataLimit(remaining)}",
                                    "Category $normalizedType  •  reserved: ${formatVoucherDataLimit(budget.committedBytes)}  •  remaining: ${formatVoucherDataLimit(remaining)}"
                                ),
                                fontSize = 12.sp,
                                color = AppPalette.Muted
                            )
                        }
                    }

                    errorText?.let {
                        Text(
                            text = it,
                            modifier = Modifier.fillMaxWidth(),
                            color = Color(0xFFC62828),
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = {
                            val saved = onSave(enabled, packageType, capacityAmount, capacityUnit)
                            if (saved) {
                                errorText = null
                                onBack()
                            } else {
                                errorText = appText(
                                    "أدخل فئة صحيحة وحجم باقة أكبر من صفر، أو أوقف حد الباقة.",
                                    "Enter a valid category and package size greater than zero, or disable the package limit."
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AppPalette.Teal)
                    ) {
                        Text(
                            text = appText("حفظ والعودة", "Save and return"),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
        }
    }
}

private fun formatPackageCapacityInput(bytes: Long, unit: VoucherDataUnit): String = BigDecimal.valueOf(bytes)
    .divide(BigDecimal.valueOf(unit.bytesMultiplier), 12, RoundingMode.DOWN)
    .stripTrailingZeros()
    .toPlainString()
