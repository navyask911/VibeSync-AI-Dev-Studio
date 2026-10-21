package com.vibesync.promptstudio.presentation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLanguageTest {
    @Test
    fun supportedLanguageTagsMatchTheLocalePicker() {
        assertEquals(listOf("en", "es", "de", "hi", "ar"), AppLanguage.entries.map { it.languageTag })
    }

    @Test
    fun languageTagLookupIsCaseInsensitiveAndAcceptsRegions() {
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromLanguageTag("ES"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromLanguageTag("de-DE"))
        assertEquals(AppLanguage.HINDI, AppLanguage.fromLanguageTag("hi-IN"))
    }

    @Test
    fun unsupportedOrMissingTagsFallBackToEnglish() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag(null))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag(""))
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromLanguageTag("fr"))
    }

    @Test
    fun onlyArabicUsesRightToLeftLayoutDirection() {
        assertTrue(AppLanguage.ARABIC.isRightToLeft)
        assertFalse(AppLanguage.ENGLISH.isRightToLeft)
        assertFalse(AppLanguage.SPANISH.isRightToLeft)
        assertFalse(AppLanguage.GERMAN.isRightToLeft)
        assertFalse(AppLanguage.HINDI.isRightToLeft)
    }
}
