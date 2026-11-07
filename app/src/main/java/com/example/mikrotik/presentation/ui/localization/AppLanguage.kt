package com.example.mikrotik.presentation.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(val code: String, val nativeName: String) {
    ARABIC("ar", "العربية"),
    ENGLISH("en", "English");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ARABIC
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ARABIC }

@Composable
fun appText(arabic: String, english: String): String =
    if (LocalAppLanguage.current == AppLanguage.ENGLISH) english else arabic
