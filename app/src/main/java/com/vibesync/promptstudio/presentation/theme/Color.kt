package com.vibesync.promptstudio.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.ColorScheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

val Emerald = Color(0xFF10A37F)
val EmeraldDark = Color(0xFF006B51)
val Ink = Color(0xFF101418)
val Mist = Color(0xFFE9F1EE)
val Coral = Color(0xFFFF1744)
val ElectricBlue = Color(0xFF2979FF)

/** WCAG relative contrast ratio, with translucent foreground colors composited over the surface. */
fun contrastRatio(foreground: Color, background: Color): Double {
    fun linear(channel: Float): Double {
        val normalized = channel.toDouble().coerceIn(0.0, 1.0)
        return if (normalized <= 0.04045) normalized / 12.92
        else ((normalized + 0.055) / 1.055).pow(2.4)
    }
    val alpha = foreground.alpha.toDouble()
    val r = linear((foreground.red * alpha + background.red * (1.0 - alpha)).toFloat())
    val g = linear((foreground.green * alpha + background.green * (1.0 - alpha)).toFloat())
    val b = linear((foreground.blue * alpha + background.blue * (1.0 - alpha)).toFloat())
    val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b
    val surfaceLuminance = 0.2126 * linear(background.red) +
        0.7152 * linear(background.green) + 0.0722 * linear(background.blue)
    val lighter = max(luminance, surfaceLuminance)
    val darker = min(luminance, surfaceLuminance)
    return (lighter + 0.05) / (darker + 0.05)
}

/** Body text must remain readable on every common Material 3 surface role used by the app. */
fun hasAccessibleBodyContrast(colors: ColorScheme): Boolean {
    val threshold = 4.5
    return contrastRatio(colors.onSurface, colors.surface) >= threshold &&
        contrastRatio(colors.onSurfaceVariant, colors.surfaceVariant) >= threshold &&
        contrastRatio(colors.onSurface, colors.surfaceContainerLow) >= threshold &&
        contrastRatio(colors.onSurface, colors.surfaceContainer) >= threshold &&
        contrastRatio(colors.onSurface, colors.surfaceContainerHigh) >= threshold &&
        contrastRatio(colors.onSurfaceVariant, colors.surfaceContainerLow) >= threshold &&
        contrastRatio(colors.onSurfaceVariant, colors.surfaceContainer) >= threshold &&
        contrastRatio(colors.onSurfaceVariant, colors.surfaceContainerHigh) >= threshold
}

/** Preserve dynamic color when readable; otherwise use the app's known-readable base scheme. */
fun ensureAccessibleColorScheme(candidate: ColorScheme, fallback: ColorScheme): ColorScheme =
    if (hasAccessibleBodyContrast(candidate)) candidate else fallback
