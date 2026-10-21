package com.vibesync.promptstudio.presentation.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserAuthRoutingTest {
    @Test
    fun routesKnownIdentityProvidersToSystemBrowser() {
        listOf(
            "accounts.google.com", "login.microsoftonline.com", "auth.openai.com",
            "auth.anthropic.com", "accounts.poe.com", "login.deepseek.com",
            "auth.perplexity.ai", "tenant.auth0.com", "sign-in.clerk.accounts.dev"
        ).forEach { host -> assertTrue("$host should use the system browser", isIdentityProviderHost(host)) }
    }

    @Test
    fun keepsAiProductSitesAndUnrelatedHostsInsideWebView() {
        listOf(
            "chatgpt.com", "gemini.google.com", "claude.ai",
            "poe.com", "chat.deepseek.com", "example.com"
        ).forEach { host -> assertFalse("$host should remain in WebView", isIdentityProviderHost(host)) }
    }

    @Test
    fun hostMatchingIgnoresCaseAndTrailingDotWithoutMatchingLookalikes() {
        assertTrue(isIdentityProviderHost("ACCOUNTS.GOOGLE.COM."))
        assertFalse(isIdentityProviderHost("accounts.google.com.attacker.example"))
        assertFalse(isIdentityProviderHost(null))
    }
}
