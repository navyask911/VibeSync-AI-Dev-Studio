package com.vibesync.promptstudio.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Uses Android's built-in typefaces. Serif provides a Times-style appearance without bundling
 * a proprietary font file.
 */
fun VibeTypography(fontChoice: String = "serif"): Typography {
    val family = when (fontChoice) {
        "sans" -> FontFamily.SansSerif
        "mono" -> FontFamily.Monospace
        else -> FontFamily.Serif
    }
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            lineHeight = 34.sp
        ),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(
            fontFamily = family,
            fontWeight = FontWeight.Bold,
            fontSize = 21.sp,
            lineHeight = 28.sp
        ),
        titleMedium = base.titleMedium.copy(
            fontFamily = family,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            lineHeight = 23.sp
        ),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 24.sp
        ),
        bodyMedium = base.bodyMedium.copy(
            fontFamily = family,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            lineHeight = 21.sp
        ),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(
            fontFamily = family,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp
        )
    )
}
