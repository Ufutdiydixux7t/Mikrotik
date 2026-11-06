package com.example.mikrotik.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

object AppPalette {
    @Volatile
    var isDarkMode: Boolean = false

    val Navy: Color get() = if (isDarkMode) Color(0xFF0A2633) else Color(0xFF123F56)
    val TextNavy: Color get() = if (isDarkMode) Color(0xFFE5F0F4) else Color(0xFF173451)
    val Teal: Color get() = if (isDarkMode) Color(0xFF0E756F) else Color(0xFF15989A)
    val Background: Color get() = if (isDarkMode) Color(0xFF10191F) else Color(0xFFF7FAF9)
    val Card: Color get() = if (isDarkMode) Color(0xFF1B2A33) else Color(0xFFFFFFFF)
    val Border: Color get() = if (isDarkMode) Color(0xFF354750) else Color(0xFFE2ECEB)
    val Mint: Color get() = if (isDarkMode) Color(0xFF22383F) else Color(0xFFE6F6F2)
    val MintStrong: Color get() = if (isDarkMode) Color(0xFF25474A) else Color(0xFFD5EEE8)
    val Positive: Color get() = if (isDarkMode) Color(0xFF69D3A8) else Color(0xFF168B70)
    val Muted: Color get() = if (isDarkMode) Color(0xFFA5B5BD) else Color(0xFF6F8190)
}
