package com.vibesync.promptstudio.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeContrastTest {

    @Test
    fun baselineLightAndDarkSchemesMeetWcagBodyContrast() {
        assertTrue(hasAccessibleBodyContrast(BaselineLightColors))
        assertTrue(hasAccessibleBodyContrast(BaselineDarkColors))
        assertTrue(contrastRatio(Color.White, Color.Black) >= 4.5)
    }

    @Test
    fun lowContrastDynamicDarkSchemeFallsBackToBaselineDark() {
        val lowContrastDynamic = darkColorScheme().copy(
            surface = Color(0xFF202020),
            onSurface = Color(0xFF282828),
            surfaceVariant = Color(0xFF202020),
            onSurfaceVariant = Color(0xFF282828),
            surfaceContainerLow = Color(0xFF202020),
            surfaceContainer = Color(0xFF202020),
            surfaceContainerHigh = Color(0xFF202020)
        )

        assertFalse(hasAccessibleBodyContrast(lowContrastDynamic))
        assertEquals(
            BaselineDarkColors,
            ensureAccessibleColorScheme(lowContrastDynamic, BaselineDarkColors)
        )
    }
}
