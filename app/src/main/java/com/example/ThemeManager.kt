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

    private var hasInitialized = false
    private lateinit var sharedPrefs: android.content.SharedPreferences

    fun init(context: Context) {
        if (hasInitialized) return
        sharedPrefs = context.applicationContext.getSharedPreferences("shikkhaloy_prefs", Context.MODE_PRIVATE)
        themeMode = sharedPrefs.getString("selected_theme_mode", "system") ?: "system"
        hasInitialized = true
    }

    fun setThemeMode(context: Context, mode: String) {
        init(context)
        themeMode = mode
        sharedPrefs.edit().putString("selected_theme_mode", mode).apply()
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
        primaryContainer = Color(0xFFEFF6FF),
        onPrimaryContainer = Color(0xFF1E3A8A),
        secondary = Color(0xFF0F766E), // Teal Green
        onSecondary = Color.White,
        secondaryContainer = Color(0xFFECFDF5),
        onSecondaryContainer = Color(0xFF065F46),
        tertiary = Color(0xFFF4B400), // Gold / Yellow
        background = Color(0xFFF8FAFC), // Off-white
        surface = Color.White,
        onBackground = Color(0xFF1E293B),
        onSurface = Color(0xFF1E293B),
        error = Color(0xFFEF4444)
    )

    val DarkColorScheme = darkColorScheme(
        primary = Color(0xFF93C5FD), // Light blue
        onPrimary = Color(0xFF1E3A8A),
        primaryContainer = Color(0xFF1E3A8A),
        onPrimaryContainer = Color(0xFFEFF6FF),
        secondary = Color(0xFF2DD4BF), // Light Teal
        onSecondary = Color(0xFF0F766E),
        secondaryContainer = Color(0xFF0F766E),
        onSecondaryContainer = Color(0xFFECFDF5),
        tertiary = Color(0xFFFCD34D),
        background = Color(0xFF0F172A), // Slate 900
        surface = Color(0xFF1E293B), // Slate 800
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFF1F5F9),
        error = Color(0xFFF87171)
    )
}
