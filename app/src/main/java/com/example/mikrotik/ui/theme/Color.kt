package com.example.mikrotik.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

data class AppPaletteColors(
    val primary: Color,
    val topBar: Color,
    val text: Color,
    val background: Color,
    val card: Color,
    val border: Color,
    val soft: Color,
    val softStrong: Color,
    val positive: Color,
    val muted: Color
)

enum class AppColorTheme(
    val code: String,
    val arabicName: String,
    val englishName: String,
    val preview: Color,
    val light: AppPaletteColors,
    val dark: AppPaletteColors
) {
    DEFAULT(
        "default", "تركوازي كلاسيكي", "Classic Teal", Color(0xFF15989A),
        AppPaletteColors(Color(0xFF15989A), Color(0xFF123F56), Color(0xFF173451), Color(0xFFF7FAF9), Color.White, Color(0xFFE2ECEB), Color(0xFFE6F6F2), Color(0xFFD5EEE8), Color(0xFF168B70), Color(0xFF6F8190)),
        AppPaletteColors(Color(0xFF42C6BC), Color(0xFF102C3A), Color(0xFFE5F0F4), Color(0xFF10191F), Color(0xFF1B2A33), Color(0xFF354750), Color(0xFF22383F), Color(0xFF25474A), Color(0xFF69D3A8), Color(0xFFA5B5BD))
    ),
    BURGUNDY_BEIGE(
        "burgundy_beige", "عنابي وبيج", "Burgundy & Beige", Color(0xFF8B344A),
        AppPaletteColors(Color(0xFF8B344A), Color(0xFF63283B), Color(0xFF432C31), Color(0xFFF4EBDD), Color(0xFFFFFCF6), Color(0xFFE3D5C5), Color(0xFFF3E4D9), Color(0xFFEBD2C7), Color(0xFF8A6842), Color(0xFF81736A)),
        AppPaletteColors(Color(0xFFE49AAF), Color(0xFF38232C), Color(0xFFF3E8E5), Color(0xFF201A1C), Color(0xFF302629), Color(0xFF514047), Color(0xFF382A31), Color(0xFF493039), Color(0xFFD9B77F), Color(0xFFC0AEB2))
    ),
    OCEAN_BLUE(
        "ocean_blue", "أزرق المحيط", "Ocean Blue", Color(0xFF2873A5),
        AppPaletteColors(Color(0xFF2873A5), Color(0xFF164467), Color(0xFF23394D), Color(0xFFF0F6FA), Color(0xFFFFFFFF), Color(0xFFD7E4EC), Color(0xFFE5F1F8), Color(0xFFD1E5F2), Color(0xFF287B6B), Color(0xFF6A7E8E)),
        AppPaletteColors(Color(0xFF77B8E4), Color(0xFF142B3A), Color(0xFFE5F1F8), Color(0xFF101A22), Color(0xFF1D2B35), Color(0xFF354956), Color(0xFF203441), Color(0xFF294455), Color(0xFF78C7A8), Color(0xFFA8BBC7))
    ),
    FOREST(
        "forest", "أخضر الغابة", "Forest Green", Color(0xFF397354),
        AppPaletteColors(Color(0xFF397354), Color(0xFF284C3A), Color(0xFF263B30), Color(0xFFF1F6EF), Color(0xFFFFFEFB), Color(0xFFD9E4D5), Color(0xFFE6F0E1), Color(0xFFD5E6CD), Color(0xFF43805B), Color(0xFF718173)),
        AppPaletteColors(Color(0xFF8BC49B), Color(0xFF1C3025), Color(0xFFE5EFE6), Color(0xFF121A15), Color(0xFF202C24), Color(0xFF3A4A3D), Color(0xFF24342A), Color(0xFF2C4332), Color(0xFF8BC49B), Color(0xFFAAB9AB))
    ),
    PURPLE(
        "purple", "بنفسجي هادئ", "Soft Purple", Color(0xFF7553A6),
        AppPaletteColors(Color(0xFF7553A6), Color(0xFF493769), Color(0xFF342C43), Color(0xFFF5F1F9), Color(0xFFFFFDFF), Color(0xFFE2D9EB), Color(0xFFF0E9F6), Color(0xFFE4D8F0), Color(0xFF73578D), Color(0xFF7D7488)),
        AppPaletteColors(Color(0xFFB99BE3), Color(0xFF2D243C), Color(0xFFEDE7F4), Color(0xFF17141C), Color(0xFF27232E), Color(0xFF453C50), Color(0xFF30283A), Color(0xFF3B3049), Color(0xFFB99BE3), Color(0xFFB4A9BC))
    ),
    ROSE(
        "rose", "وردي ترابي", "Dusty Rose", Color(0xFFB65F72),
        AppPaletteColors(Color(0xFFB65F72), Color(0xFF75414D), Color(0xFF49343A), Color(0xFFFAF1F0), Color(0xFFFFFCFB), Color(0xFFEBDADB), Color(0xFFF7E8E7), Color(0xFFF0D7D8), Color(0xFF9A6A58), Color(0xFF887579)),
        AppPaletteColors(Color(0xFFE49AA5), Color(0xFF3C282D), Color(0xFFF4E8E9), Color(0xFF21191B), Color(0xFF302528), Color(0xFF514047), Color(0xFF392A2F), Color(0xFF493238), Color(0xFFE2B092), Color(0xFFC0ADB0))
    );

    companion object {
        fun fromCode(code: String?): AppColorTheme = entries.firstOrNull { it.code == code } ?: DEFAULT
    }
}

object AppPalette {
    @Volatile
    var isDarkMode: Boolean = false

    @Volatile
    var currentTheme: AppColorTheme = AppColorTheme.DEFAULT

    private val colors: AppPaletteColors
        get() = if (isDarkMode) currentTheme.dark else currentTheme.light

    val Navy: Color get() = colors.topBar
    val TextNavy: Color get() = colors.text
    val Teal: Color get() = colors.primary
    val Background: Color get() = colors.background
    val Card: Color get() = colors.card
    val Border: Color get() = colors.border
    val Mint: Color get() = colors.soft
    val MintStrong: Color get() = colors.softStrong
    val Positive: Color get() = colors.positive
    val Muted: Color get() = colors.muted
}
