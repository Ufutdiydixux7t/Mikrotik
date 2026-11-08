package com.example.mikrotik.core.captive

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class PortalAppearance(
    val name: String,
    val theme: PortalTheme
)

enum class PortalTheme(
    val storageKey: String,
    val displayName: String,
    val accent: String,
    val accentDark: String,
    val tint: String,
    val waveLight: String,
    val waveMid: String,
    val waveDark: String,
    val waveLine: String,
    val buttonShadow: String
) {
    GREEN(
        "green", "أخضر (الافتراضي)", "#138D86", "#0E756F", "#DDF1ED",
        "#E7F5F2", "#B9E3DA", "#168F87", "#7ACCC2", "rgba(19,141,134,0.18)"
    ),
    BLUE(
        "blue", "أزرق", "#1976D2", "#125AA0", "#E1EFFD",
        "#EAF4FE", "#C5DDF7", "#2878C7", "#8AB9F0", "rgba(25,118,210,0.18)"
    ),
    RED(
        "red", "أحمر", "#D84343", "#B52D36", "#FCE8E9",
        "#FFF0F0", "#F4C9CA", "#CC4249", "#E99A9D", "rgba(216,67,67,0.18)"
    ),
    PINK(
        "pink", "وردي", "#C2185B", "#991548", "#FCE4EC",
        "#FFF2F7", "#F4BDD1", "#C2185B", "#E889AE", "rgba(194,24,91,0.18)"
    ),
    PURPLE(
        "purple", "بنفسجي", "#6B4FA1", "#523A80", "#EEE6FA",
        "#F7F3FC", "#D7C8EF", "#7558AA", "#B49AD6", "rgba(107,79,161,0.18)"
    ),
    ORANGE(
        "orange", "برتقالي", "#B45309", "#8F4207", "#FFF0E4",
        "#FFF8F0", "#F9D4B5", "#B45309", "#E8A06B", "rgba(180,83,9,0.18)"
    );

    companion object {
        fun fromStorageKey(key: String?): PortalTheme =
            values().firstOrNull { it.storageKey == key } ?: GREEN
    }
}

@Singleton
class PortalAppearanceSettings @Inject constructor(
    @ApplicationContext context: Context
) {
    private val preferences = context.getSharedPreferences("portal_appearance", Context.MODE_PRIVATE)

    fun current(): PortalAppearance {
        val name = preferences.getString(KEY_NAME, DEFAULT_NAME)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: DEFAULT_NAME
        val theme = PortalTheme.fromStorageKey(preferences.getString(KEY_THEME, PortalTheme.GREEN.storageKey))
        return PortalAppearance(name = name, theme = theme)
    }

    fun saveName(name: String) {
        preferences.edit().putString(KEY_NAME, name.take(MAX_NAME_LENGTH)).apply()
    }

    fun saveTheme(theme: PortalTheme) {
        preferences.edit().putString(KEY_THEME, theme.storageKey).apply()
    }

    companion object {
        const val DEFAULT_NAME = "AL-MADDI NET"
        const val MAX_NAME_LENGTH = 40
        private const val KEY_NAME = "portal_name"
        private const val KEY_THEME = "portal_theme"
    }
}
