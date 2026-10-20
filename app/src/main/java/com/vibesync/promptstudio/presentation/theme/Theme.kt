package com.vibesync.promptstudio.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Emerald,
    secondary = ElectricBlue,
    tertiary = Coral,
    background = Ink,
    surface = Color(0xFF171D22)
)

private val LightColors = lightColorScheme(
    primary = EmeraldDark,
    secondary = ElectricBlue,
    tertiary = Coral,
    background = Mist,
    surface = Color(0xFFFAFCFB)
)

@Composable
fun VibeSyncTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> dynamicDarkColorScheme(context)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !dark -> dynamicLightColorScheme(context)
        dark -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colors, typography = VibeTypography, content = content)
}
