package com.example.mikrotik.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun MikrotikTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    AppPalette.isDarkMode = darkTheme

    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = AppPalette.Teal,
            secondary = AppPalette.Navy,
            tertiary = AppPalette.MintStrong,
            background = AppPalette.Background,
            surface = AppPalette.Card,
            onPrimary = AppPalette.Navy,
            onSecondary = AppPalette.TextNavy,
            onTertiary = AppPalette.TextNavy,
            onBackground = AppPalette.TextNavy,
            onSurface = AppPalette.TextNavy
        )
    } else {
        lightColorScheme(
            primary = AppPalette.Teal,
            secondary = AppPalette.Navy,
            tertiary = AppPalette.MintStrong,
            background = AppPalette.Background,
            surface = AppPalette.Card,
            onPrimary = androidx.compose.ui.graphics.Color.White,
            onSecondary = androidx.compose.ui.graphics.Color.White,
            onBackground = AppPalette.TextNavy,
            onSurface = AppPalette.TextNavy
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
