package com.example

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

object ThemeManager {
    var themeMode by mutableStateOf("system") // "light", "dark", "system"
    var isPipEnabled by mutableStateOf(true)

    private var hasInitialized = false
    private lateinit var sharedPrefs: android.content.SharedPreferences

    fun init(context: Context) {
        if (hasInitialized) return
        sharedPrefs = PrefUtils.getSecurePrefs(context)
        themeMode = sharedPrefs.getString("selected_theme_mode", "system") ?: "system"
        isPipEnabled = sharedPrefs.getBoolean("pip_enabled", true)
        hasInitialized = true
    }

    fun setThemeMode(context: Context, mode: String) {
        init(context)
        themeMode = mode
        sharedPrefs.edit().putString("selected_theme_mode", mode).apply()
    }

    fun setPipEnabled(context: Context, enabled: Boolean) {
        init(context)
        isPipEnabled = enabled
        sharedPrefs.edit().putBoolean("pip_enabled", enabled).apply()
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return when (themeMode) {
            "light" -> false
            "dark" -> true
            else -> isSystemInDarkTheme()
        }
    }

    val LightColorScheme = lightColorScheme(
        primary = Color(0xFF1E3A8A), // Navy Blue
        onPrimary = Color.White,
        primaryContainer = Color(0xFFDBEAFE),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF0F766E),
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFCCFBF1),
        onSecondaryContainer = Color(0xFF0F766E),
        tertiary = Color(0xFFD97706),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        onBackground = Color(0xFF1E293B),
        onSurface = Color(0xFF1E293B),
        error = Color(0xFFEF4444)
    )

    val DarkColorScheme = darkColorScheme(
        primary = Color(0xFF93C5FD), // Light blue
        onPrimary = Color(0xFF0F172A),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFDBEAFE),
        secondary = Color(0xFF5EEAD4), // Light Teal
        onSecondary = Color(0xFF0F172A),
        secondaryContainer = Color(0xFF0F766E),
        onSecondaryContainer = Color(0xFFCCFBF1),
        tertiary = Color(0xFFFBBF24),
        background = Color(0xFF0F172A), // Slate 900
        surface = Color(0xFF1E293B), // Slate 800
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        error = Color(0xFFEF4444)
    )
}

object VideoPipState {
    var isVideoActive by mutableStateOf(false)
    var isInPip by mutableStateOf(false)
    var onEnterPip: (() -> Unit)? = null

    var isPlaying by mutableStateOf(true)
    var onPlayPauseToggle: ((Boolean) -> Unit)? = null
    var onRewind: (() -> Unit)? = null
    var onForward: (() -> Unit)? = null
}
