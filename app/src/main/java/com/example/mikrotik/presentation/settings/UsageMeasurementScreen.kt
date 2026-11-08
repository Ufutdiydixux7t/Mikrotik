package com.example.mikrotik.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        OutlinedButton(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = HeaderColor)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(appText("رجوع إلى الإعدادات", "Back to settings"))
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
            }
        }

        Text(
            text = appText(
                "تعرض إجمالي حركة واجهة الإنترنت، ومجموع استهلاك الأجهزة المتصلة، وتقدير ما استهلكه الهاتف نفسه.",
                "Shows total active-upstream traffic, connected-device usage, and an estimate of the phone's own usage."
            ),
            color = AppPalette.Muted,
            fontSize = 13.sp,
            lineHeight = 20.sp
        )

        MeasurementCard(
            icon = AppIcons.BarChart,
            title = appText("إجمالي الاستهلاك من مصدر الإنترنت اليوم", "Total usage from Internet source today"),
            value = formatBytes(snapshot.sourceTodayBytes),
            detail = appText(
                if (snapshot.sourceCounterAvailable) {
                    "عداد واجهة الإنترنت النشطة، ويشمل الهاتف والأجهزة التي يمرر لها الاتصال."
                } else {
                    "العداد غير متاح حالياً؛ تحقق من اتصال الهاتف بالإنترنت."
                },
                if (snapshot.sourceCounterAvailable) {
                    "Active Internet-interface counter; includes the phone and devices routed through it."
                } else {
                    "Counter is currently unavailable; check the phone's Internet connection."
                }
            )
        )

        MeasurementCard(
            icon = AppIcons.CreditCard,
            title = appText("استهلاك الأجهزة الأخرى اليوم", "Other devices' usage today"),
            value = formatBytes(snapshot.clientsTodayBytes),
            detail = appText(
                "مجموع ما سجّلته عدادات جلسات الأجهزة المتصلة ببطاقات صالحة. الإجمالي منذ التصفير: ${formatBytes(snapshot.clientsTotalBytes)}",
                "Sum recorded for connected devices with valid voucher sessions. Since reset: ${formatBytes(snapshot.clientsTotalBytes)}"
            )
        )

        MeasurementCard(
            icon = AppIcons.Smartphone,
            title = appText("استهلاك الهاتف وتطبيقاته اليوم (تقديري)", "Phone and apps usage today (estimated)"),
            value = if (snapshot.sourceCounterAvailable || snapshot.sourceTodayBytes > 0L) {
                formatBytes(snapshot.phoneEstimateTodayBytes)
            } else {
                appText("غير متاح", "Unavailable")
            },
            detail = appText(
                "تقدير الهاتف = إجمالي المصدر − استهلاك الأجهزة الأخرى.",
                "Phone estimate = source total − other devices' usage."
            )
        )

        if (snapshot.clientsExceedSourceBytes > 0L) {
            Text(
                text = appText(
                    "تنبيه: عداد الأجهزة أعلى من عداد المصدر بـ ${formatBytes(snapshot.clientsExceedSourceBytes)}. قد تختلف مصادر القياس أو توقيتها؛ لذلك يظهر استهلاك الهاتف صفراً بدلاً من رقم سالب.",
                    "Warning: the client counter exceeds the source counter by ${formatBytes(snapshot.clientsExceedSourceBytes)}. Meter sources or timing may differ, so phone usage is shown as zero rather than a negative value."
                ),
                color = Color(0xFFB3261E),
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
        }

        Text(
            text = appText(
                "الأجهزة الأخرى تُحسب من عدادات جلساتها النشطة؛ إذا تعذر عداد جهاز فقد يظهر استهلاكه ضمن الفرق المنسوب للهاتف. قد يوجد فرق بسيط بسبب توقيت القياس.",
                "Other devices are measured from active session counters; if a device counter is unavailable, its usage may appear in the phone estimate. Small timing differences are also possible."
            ),
            color = AppPalette.Muted,
            fontSize = 11.sp,
            lineHeight = 17.sp
        )

        Button(
            onClick = { showResetConfirmation = true },
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB3261E))
        ) {
            Icon(AppIcons.Delete, contentDescription = null, modifier = Modifier.size(19.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                text = appText("تصفير أرقام الاستهلاك", "Reset usage counters"),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        resetMessage?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth(),
                color = AccentColor,
                textAlign = TextAlign.Center,
                fontSize = 13.sp
            )
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showResetConfirmation = false },
            title = {
                Text(appText("تصفير أرقام الاستهلاك؟", "Reset usage counters?"), color = HeaderColor)
            },
            text = {
                Text(
                    appText(
                        "سيُصفّر إجمالي المصدر واستهلاك الأجهزة وتقدير الهاتف فقط. لن تُحذف البطاقات ولن تتغير حصصها؛ يبدأ القياس من جديد بعد التصفير.",
                        "Only the source total, client usage, and phone estimate will be reset. Vouchers and their limits will not change; measurement starts again afterward."
                    ),
                    color = AppPalette.Muted
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sourceCounterAvailable = UsageMeasurementStore.reset(context)
                        snapshot = UsageMeasurementStore.read(context)
                        resetMessage = if (sourceCounterAvailable) {
                            appText("تم تصفير الأرقام وإعادة معايرة عداد المصدر.", "Counters reset and the source counter has been re-baselined.")
                        } else {
                            appText("تم التصفير؛ سيبدأ القياس عند توفر اتصال إنترنت نشط.", "Reset complete; measurement will resume when an active Internet connection is available.")
                        }
                        onResetComplete()
                        showResetConfirmation = false
                    }
                ) {
                    Text(appText("تصفير", "Reset"), color = Color(0xFFB3261E))
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
private fun MeasurementCard(
    icon: ImageVector,
    title: String,
    value: String,
    detail: String
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentColor,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentColor.copy(alpha = 0.10f))
                        .padding(8.dp)
                )
                Text(
                    text = title,
                    color = HeaderColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = value,
                color = HeaderColor,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = detail,
                color = AppPalette.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp
            )
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
