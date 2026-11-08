package com.example.mikrotik.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.core.network.UsageMeasurementStore
import com.example.mikrotik.presentation.ui.localization.AppLanguage
import com.example.mikrotik.presentation.ui.localization.LocalAppLanguage
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

private val HeaderColor get() = AppPalette.TextNavy
private val CardColor get() = AppPalette.Card
private val AccentColor get() = AppPalette.Teal
private val BorderColor get() = AppPalette.Border

@Composable
fun UsageMeasurementScreen(
    onBack: () -> Unit,
    onResetComplete: () -> Unit
) {
    val context = LocalContext.current
    val msgResetRestarted = appText(
        "تم تصفير الأرقام وإعادة معايرة عداد المصدر.",
        "Counters reset and the source counter has been re-baselined."
    )
    val msgResetResuming = appText(
        "تم التصفير؛ سيبدأ القياس عند توفر اتصال إنترنت نشط.",
        "Reset complete; measurement will resume when an active Internet connection is available."
    )
    var snapshot by remember(context) { mutableStateOf(UsageMeasurementStore.read(context)) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var resetMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(context) {
        while (isActive) {
            UsageMeasurementStore.sampleSourceTraffic(context)
            snapshot = UsageMeasurementStore.read(context)
            delay(1_000L)
        }
    }

    val sourceValue = if (snapshot.sourceCounterAvailable || snapshot.sourceTodayBytes > 0L) {
        formatBytes(snapshot.sourceTodayBytes)
    } else {
        appText("غير متاح", "Unavailable")
    }
    val phoneValue = if (snapshot.sourceCounterAvailable || snapshot.sourceTodayBytes > 0L) {
        formatBytes(snapshot.phoneEstimateTodayBytes)
    } else {
        appText("غير متاح", "Unavailable")
    }
    val liveLabel = if (snapshot.sourceCounterAvailable) {
        appText("يُحدّث الآن", "Live")
    } else {
        appText("بانتظار الاتصال", "Waiting for network")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderColor)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    imageVector = if (LocalAppLanguage.current == AppLanguage.ARABIC) {
                        AppIcons.ChevronRight
                    } else {
                        AppIcons.ChevronLeft
                    },
                    contentDescription = null,
                    tint = HeaderColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(appText("رجوع إلى الإعدادات", "Back to settings"))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Text(
                text = appText("ملخص استهلاك الإنترنت", "Internet usage overview"),
                fontSize = 21.sp,
                fontWeight = FontWeight.Bold,
                color = HeaderColor
            )
            Text(
                text = appText(
                    "قارن حركة المصدر باستهلاك الأجهزة وتقدير الهاتف لهذا اليوم.",
                    "Compare source traffic, connected devices, and estimated phone usage for today."
                ),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = AppPalette.Muted
            )
        }

        SourceTotalCard(
            value = sourceValue,
            status = liveLabel,
            detail = appText(
                "إجمالي البيانات عبر واجهة الإنترنت النشطة، ويشمل الهاتف والأجهزة التي يمرر لها الاتصال.",
                "Total data through the active Internet interface, including the phone and devices routed through it."
            )
        )

        Text(
            text = appText("تفصيل الاستهلاك اليوم", "Today's breakdown"),
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = HeaderColor
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            CompactUsageCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.CreditCard,
                title = appText("الأجهزة الأخرى", "Other devices"),
                value = formatBytes(snapshot.clientsTodayBytes),
                detail = appText(
                    "اليوم • منذ التصفير ${formatBytes(snapshot.clientsTotalBytes)}",
                    "Today • since reset ${formatBytes(snapshot.clientsTotalBytes)}"
                ),
                accent = AppPalette.Teal
            )
            CompactUsageCard(
                modifier = Modifier.weight(1f),
                icon = AppIcons.Smartphone,
                title = appText("الهاتف (تقديري)", "Phone (estimated)"),
                value = phoneValue,
                detail = appText("الإجمالي ناقص الأجهزة", "Source total minus devices"),
                accent = AppPalette.Positive
            )
        }

        if (snapshot.clientsExceedSourceBytes > 0L) {
            UsageNoticeCard(
                warning = true,
                text = appText(
                    "عداد الأجهزة أعلى من إجمالي المصدر بـ ${formatBytes(snapshot.clientsExceedSourceBytes)}. قد تختلف العدادات أو توقيت أخذ العينات؛ لذلك لا يظهر تقدير الهاتف كقيمة سالبة.",
                    "The device counter exceeds the source total by ${formatBytes(snapshot.clientsExceedSourceBytes)}. Meter sources or sampling times may differ, so the phone estimate is not shown as a negative value."
                )
            )
        } else {
            UsageNoticeCard(
                warning = false,
                text = appText(
                    "تقدير الهاتف = إجمالي المصدر − استهلاك الأجهزة. إذا تعذر عداد جهاز متصل فقد يظهر جزء من استهلاكه ضمن التقدير.",
                    "Phone estimate = source total − device usage. If a connected device counter is unavailable, some of its usage may appear in the estimate."
                )
            )
        }

        OutlinedButton(
            onClick = { showResetConfirmation = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, if (AppPalette.isDarkMode) Color(0xFFFF8A80) else Color(0xFFB3261E)),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = if (AppPalette.isDarkMode) Color(0xFFFFB4AB) else Color(0xFFB3261E)
            )
        ) {
            Icon(AppIcons.Delete, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = appText("تصفير أرقام القياس", "Reset measurements"),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        resetMessage?.let { message ->
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = AppPalette.Mint
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = AppIcons.Info,
                        contentDescription = null,
                        tint = AppPalette.Positive,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message,
                        modifier = Modifier.weight(1f),
                        color = HeaderColor,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(appText("تصفير أرقام القياس؟", "Reset usage measurements?"), color = HeaderColor)
            },
            text = {
                Text(
                    appText(
                        "سيُصفّر إجمالي المصدر واستهلاك الأجهزة وتقدير الهاتف فقط. لن تُحذف البطاقات ولن تتغير حصصها؛ يبدأ القياس من جديد بعد التصفير.",
                        "Only the source total, device usage, and phone estimate will be reset. Vouchers and their limits will not change; measurement starts again afterward."
                    ),
                    color = AppPalette.Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sourceCounterAvailable = UsageMeasurementStore.reset(context)
                        snapshot = UsageMeasurementStore.read(context)
                        resetMessage = if (sourceCounterAvailable) msgResetRestarted else msgResetResuming
                        onResetComplete()
                        showResetConfirmation = false
                    }
                ) {
                    Text(appText("تصفير", "Reset"), color = if (AppPalette.isDarkMode) Color(0xFFFFB4AB) else Color(0xFFB3261E))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmation = false }) {
                    Text(appText("إلغاء", "Cancel"), color = HeaderColor)
                }
            },
            containerColor = CardColor
        )
    }
}

@Composable
private fun SourceTotalCard(value: String, status: String, detail: String) {
    val shape = RoundedCornerShape(22.dp)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF123451)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF173957), Color(0xFF087E80))
                    )
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = appText("إجمالي المصدر اليوم", "Source total today"),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White.copy(alpha = 0.92f)
                    )
                    Surface(
                        shape = RoundedCornerShape(30.dp),
                        color = Color.White.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color.White.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = AppIcons.WifiSignal,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Text(
                text = value,
                fontSize = 35.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HorizontalDivider(color = Color.White.copy(alpha = 0.22f))
            Text(
                text = detail,
                fontSize = 11.sp,
                lineHeight = 17.sp,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun CompactUsageCard(
    modifier: Modifier,
    icon: ImageVector,
    title: String,
    value: String,
    detail: String,
    accent: Color
) {
    val shape = RoundedCornerShape(18.dp)
    Card(
        modifier = modifier
            .heightIn(min = 168.dp)
            .border(1.dp, BorderColor, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = if (AppPalette.isDarkMode) 0.20f else 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
            }
            Text(
                text = title,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = HeaderColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                fontSize = 20.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                color = HeaderColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = detail,
                fontSize = 10.sp,
                lineHeight = 15.sp,
                color = AppPalette.Muted,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun UsageNoticeCard(text: String, warning: Boolean) {
    val background = when {
        warning && AppPalette.isDarkMode -> Color(0xFF44282B)
        warning -> Color(0xFFFFF1F0)
        else -> AppPalette.Mint
    }
    val foreground = when {
        warning && AppPalette.isDarkMode -> Color(0xFFFFB4AB)
        warning -> Color(0xFF9F2720)
        else -> AppPalette.Muted
    }
    val iconColor = if (warning) foreground else AppPalette.Positive
    val shape = RoundedCornerShape(14.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, if (warning) foreground.copy(alpha = 0.22f) else BorderColor, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = if (warning) AppIcons.Info else AppIcons.Shield,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(19.dp)
            )
            Text(text, fontSize = 11.sp, lineHeight = 17.sp, color = foreground)
        }
    }
}

private fun formatBytes(bytes: Long): String {
    val safeBytes = bytes.coerceAtLeast(0L)
    if (safeBytes < 1024L) return "$safeBytes B"
    val units = listOf("KB", "MB", "GB", "TB")
    var value = safeBytes.toDouble()
    var unitIndex = -1
    do {
        value /= 1024.0
        unitIndex++
    } while (value >= 1024.0 && unitIndex < units.lastIndex)
    return String.format(Locale.ROOT, "%.2f %s", value, units[unitIndex])
}
