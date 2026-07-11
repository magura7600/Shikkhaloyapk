package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = DarkNavy,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFCBD5E1)
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    secondary = Secondary,
    tertiary = Tertiary,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.Black,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A),
    onSurfaceVariant = Color(0xFF334155)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.setDecorFitsSystemWindows(window, false)
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }
    
    val context = LocalContext.current
    val safeFontFamily = remember(context) {
        try {
            // Pre-verify that the system can load custom font resources without throwing
            val typeface = ResourcesCompat.getFont(context, com.example.R.font.hind_siliguri_regular)
            if (typeface != null) {
                FontFamily(
                    Font(com.example.R.font.hind_siliguri_regular, FontWeight.Normal),
                    Font(com.example.R.font.hind_siliguri_medium, FontWeight.Medium),
                    Font(com.example.R.font.hind_siliguri_bold, FontWeight.Bold)
                )
            } else {
                FontFamily.SansSerif
            }
        } catch (e: Throwable) {
            // Graceful fallback to SansSerif on bugged Samsung Android 13/14 custom system fonts
            android.util.Log.e("MyApplicationTheme", "System custom font engine failed, using safe fallback", e)
            FontFamily.SansSerif
        }
    }

    val dynamicTypography = remember(safeFontFamily) {
        Typography.copy(
            bodyLarge = Typography.bodyLarge.copy(fontFamily = safeFontFamily),
            bodyMedium = Typography.bodyMedium.copy(fontFamily = safeFontFamily),
            titleLarge = Typography.titleLarge.copy(fontFamily = safeFontFamily),
            titleMedium = Typography.titleMedium.copy(fontFamily = safeFontFamily),
            labelLarge = Typography.labelLarge.copy(fontFamily = safeFontFamily)
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = dynamicTypography,
        shapes = Shapes,
        content = content
    )
}
