package com.vibesync.promptstudio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun supportedLanguageTagsAreStableAndOrdered() {
        assertEquals(listOf("en", "es", "de", "hi", "ar"), supportedAppLanguages.map { it.tag })
    }

    @Test
    fun languageTagsNormalizeRegionsAndUnsupportedLocales() {
        assertEquals("es", normalizeAppLanguageTag("es-ES"))
        assertEquals("ar", normalizeAppLanguageTag("ar-EG,es"))
        assertEquals("en", normalizeAppLanguageTag("fr-FR"))
        assertEquals("en", normalizeAppLanguageTag(""))
    }

    @Test
    fun onlyArabicUsesRightToLeftLayoutAmongSupportedLanguages() {
        assertTrue(isAppLanguageRtl("ar"))
        assertTrue(isAppLanguageRtl("ar-SA"))
        assertFalse(isAppLanguageRtl("en"))
        assertFalse(isAppLanguageRtl("hi"))
    }
}
