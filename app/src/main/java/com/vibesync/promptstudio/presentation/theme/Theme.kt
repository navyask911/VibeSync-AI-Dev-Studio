package com.vibesync.promptstudio.presentation.theme

import android.content.SharedPreferences
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

internal val BaselineDarkColors = darkColorScheme(
    primary = Emerald, secondary = ElectricBlue, tertiary = Coral,
    background = Ink, surface = Color(0xFF171D22)
)
internal val BaselineLightColors = lightColorScheme(
    primary = EmeraldDark, secondary = ElectricBlue, tertiary = Coral,
    background = Mist, surface = Color(0xFFFAFCFB)
)

@Composable
fun VibeSyncTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val preferences = remember(context) { context.getSharedPreferences("vibesync_appearance", 0) }
    var appearance by remember(preferences) { mutableStateOf(preferences.getString("theme_mode", "normal") ?: "normal") }
    var fontChoice by remember(preferences) { mutableStateOf(preferences.getString("font_family", "serif") ?: "serif") }
    DisposableEffect(preferences) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            when (key) {
                "theme_mode" -> appearance = prefs?.getString("theme_mode", "normal") ?: "normal"
                "font_family" -> fontChoice = prefs?.getString("font_family", "serif") ?: "serif"
            }
        }
        preferences.registerOnSharedPreferenceChangeListener(listener)
        onDispose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    val systemDark = isSystemInDarkTheme()
    val dark = when (appearance) { "developer_dark" -> true; "normal" -> false; else -> systemDark }
    val colors = when (appearance) {
        "developer_dark" -> BaselineDarkColors
        "normal" -> BaselineLightColors
        else -> when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && dark -> ensureAccessibleColorScheme(dynamicDarkColorScheme(context), BaselineDarkColors)
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> ensureAccessibleColorScheme(dynamicLightColorScheme(context), BaselineLightColors)
            dark -> BaselineDarkColors
            else -> BaselineLightColors
        }
    }
    MaterialTheme(colorScheme = colors, typography = VibeTypography(fontChoice), content = content)
}
