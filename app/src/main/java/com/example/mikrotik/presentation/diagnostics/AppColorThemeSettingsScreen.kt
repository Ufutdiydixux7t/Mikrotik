package com.example.mikrotik.presentation.diagnostics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mikrotik.presentation.ui.components.AppTopBar
import com.example.mikrotik.presentation.ui.localization.appText
import com.example.mikrotik.presentation.ui.theme.AppIcons
import com.example.mikrotik.ui.theme.AppColorTheme
import com.example.mikrotik.ui.theme.AppPalette

@Composable
fun AppColorThemeSettingsScreen(
    selectedTheme: AppColorTheme,
    onThemeSelected: (AppColorTheme) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        AppTopBar(
            title = appText("ألوان التطبيق", "App colors"),
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
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = appText(
                    "اختر مجموعة ألوان متناسقة للخلفية والأزرار والأيقونات والبطاقات. تحفظ تلقائياً.",
                    "Choose a coordinated palette for backgrounds, buttons, icons, and cards. Your choice is saved automatically."
                ),
                color = AppPalette.Muted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            AppColorTheme.entries.forEach { theme ->
                val palette = if (AppPalette.isDarkMode) theme.dark else theme.light
                val selected = theme == selectedTheme
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onThemeSelected(theme) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = AppPalette.Card),
                    border = BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) palette.primary else AppPalette.Border
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = appText(theme.arabicName, theme.englishName),
                                color = AppPalette.TextNavy,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(10.dp)
                                    .clip(CircleShape),
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                    listOf(palette.primary, palette.topBar, palette.background, palette.softStrong).forEach { color ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                }
                            }
                        }
                        RadioButton(
                            selected = selected,
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = palette.primary,
                                unselectedColor = AppPalette.Muted
                            ),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }

            Text(
                text = appText(
                    "ملاحظة: ألوان صفحة تسجيل الدخول للأجهزة (البوابة) تُدار من إعداداتها بشكل مستقل.",
                    "Note: The captive login page colors are managed separately in its own settings."
                ),
                color = AppPalette.Muted,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
            )
        }
    }
}
