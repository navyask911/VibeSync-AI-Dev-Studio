package com.vibesync.promptstudio.presentation.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderAuthRoutingTest {
    @Test
    fun identityProviderAuthorizationRoutesToSystemBrowser() {
        assertTrue(isProviderAuthenticationUrl("https://accounts.google.com/o/oauth2/v2/auth"))
        assertTrue(isProviderAuthenticationUrl("https://auth.openai.com/authorize"))
        assertTrue(isProviderAuthenticationUrl("https://login.microsoftonline.com/common/oauth2/v2.0/authorize"))
    }

    @Test
    fun supportedAiProviderLoginEntryPointsRouteToSystemBrowser() {
        assertTrue(isProviderAuthenticationUrl("https://claude.ai/login"))
        assertTrue(isProviderAuthenticationUrl("https://poe.com/oauth/authorize?response_type=code"))
        assertTrue(isProviderAuthenticationUrl("https://chat.deepseek.com/sign_in"))
        assertTrue(isProviderAuthenticationUrl("https://chatgpt.com/auth/login"))
    }

    @Test
    fun ordinaryProviderPagesAndUntrustedHostsStayInWebView() {
        assertFalse(isProviderAuthenticationUrl("https://chatgpt.com/share/example"))
        assertFalse(isProviderAuthenticationUrl("https://claude.ai/new"))
        assertFalse(isProviderAuthenticationUrl("https://example.com/login"))
        assertFalse(isProviderAuthenticationUrl("javascript:alert(1)"))
        assertFalse(isProviderAuthenticationUrl("not a url"))
    }
}
