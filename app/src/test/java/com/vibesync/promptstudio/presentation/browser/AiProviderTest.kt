package com.vibesync.promptstudio.presentation.browser

import org.junit.Assert.*
import org.junit.Test

class AiProviderTest {
    @Test fun providerCatalogHasThirteenSites() { assertEquals(13, AiProvider.entries.count { it != AiProvider.CUSTOM }) }
    @Test fun allKnownSitesUseHttps() { AiProvider.entries.filter { it != AiProvider.CUSTOM }.forEach { assertTrue(it.startUrl.startsWith("https://")) } }
    @Test fun promptInjectionIsRestrictedToCatalogHosts() {
        assertTrue(AiProvider.GEMINI.supportsPromptInjection("https://gemini.google.com/app"))
        assertTrue(AiProvider.CHATGPT.supportsPromptInjection("https://chatgpt.com/"))
        assertFalse(AiProvider.CUSTOM.supportsPromptInjection("https://example.com"))
        assertFalse(AiProvider.GEMINI.supportsPromptInjection("https://gemini.google.com.attacker.example/"))
    }
}
