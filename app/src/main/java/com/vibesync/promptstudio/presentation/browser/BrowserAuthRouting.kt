package com.vibesync.promptstudio.presentation.browser

/**
 * Identity providers must authenticate in a system-controlled browser, not an app WebView.
 * AI product pages themselves remain embedded unless they navigate to an identity-provider host.
 */
internal fun isIdentityProviderHost(host: String?): Boolean {
    val normalized = host?.trim()?.trimEnd('.')?.lowercase().orEmpty()
    if (normalized.isBlank()) return false
    return normalized in identityProviderHosts ||
        normalized.endsWith(".auth0.com") ||
        normalized.endsWith(".clerk.accounts.dev")
}

private val identityProviderHosts = setOf(
    "accounts.google.com",
    "oauth2.googleapis.com",
    "login.microsoftonline.com",
    "login.live.com",
    "appleid.apple.com",
    "auth.openai.com",
    "login.openai.com",
    "auth.anthropic.com",
    "auth.poe.com",
    "accounts.poe.com",
    "auth.deepseek.com",
    "login.deepseek.com",
    "auth.perplexity.ai",
    "auth.mistral.ai"
)
