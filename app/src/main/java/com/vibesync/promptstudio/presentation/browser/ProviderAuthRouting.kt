package com.vibesync.promptstudio.presentation.browser

import java.net.URI

/**
 * Returns true when an authentication navigation must leave the embedded WebView.
 *
 * OAuth providers can reject app-controlled WebViews. These URLs are opened in the
 * system browser/Custom Tabs without modifying or disguising the WebView user agent.
 */
internal fun isProviderAuthenticationUrl(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl) }.getOrNull() ?: return false
    val scheme = uri.scheme?.lowercase() ?: return false
    if (scheme != "http" && scheme != "https") return false

    val host = uri.host?.lowercase() ?: return false
    val path = uri.path.orEmpty().lowercase()

    val identityProviderHost = host == "accounts.google.com" ||
        host == "login.microsoftonline.com" ||
        host == "login.live.com" ||
        host == "appleid.apple.com" ||
        host == "auth.openai.com" ||
        host == "auth.anthropic.com" ||
        host == "auth.poe.com" ||
        host == "auth.deepseek.com" ||
        host.endsWith(".auth0.com")

    if (identityProviderHost) return true

    return when (host) {
        "claude.ai", "www.claude.ai" ->
            path == "/login" || path.startsWith("/login/") || path.startsWith("/auth/")
        "poe.com", "www.poe.com" ->
            path == "/login" || path.startsWith("/login/") || path.startsWith("/oauth/authorize")
        "chat.deepseek.com" ->
            path == "/sign_in" || path.startsWith("/api/v0/users/oauth/")
        "chatgpt.com", "www.chatgpt.com", "chat.openai.com" ->
            path == "/login" || path.startsWith("/auth/login")
        else -> false
    }
}
