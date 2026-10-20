package com.vibesync.promptstudio.presentation.auth

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import com.vibesync.promptstudio.BuildConfig
import com.vibesync.promptstudio.data.security.EncryptedVault
import com.vibesync.promptstudio.data.security.PkceUtil
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest

/**
 * Generic public-client OAuth 2.0 Authorization Code + PKCE plumbing.
 * A provider-issued client ID and token endpoint are required at build time. An OAuth identity
 * token is never treated as a provider API key. The default build deliberately has OAuth disabled.
 */
class OAuthManager(
    private val vault: EncryptedVault,
    private val httpClient: HttpClient
) {
    val isConfigured: Boolean
        get() = BuildConfig.OAUTH_CLIENT_ID.isNotBlank() &&
            isHttpsEndpoint(BuildConfig.OAUTH_AUTHORIZE_ENDPOINT) &&
            isHttpsEndpoint(BuildConfig.OAUTH_TOKEN_ENDPOINT)

    suspend fun launch(context: Context) {
        check(isConfigured) {
            "OAuth is not configured. Set VIBESYNC_OAUTH_CLIENT_ID, VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT, and VIBESYNC_OAUTH_TOKEN_ENDPOINT to values issued for your app."
        }
        val verifier = PkceUtil.generateCodeVerifier()
        val state = PkceUtil.generateState()
        withContext(Dispatchers.IO) {
            vault.putSecret(EncryptedVault.OAUTH_PENDING_VERIFIER, verifier)
            vault.putSecret(EncryptedVault.OAUTH_PENDING_STATE, state)
        }
        val challenge = PkceUtil.generateCodeChallenge(verifier)
        val authorizeUri = Uri.parse(BuildConfig.OAUTH_AUTHORIZE_ENDPOINT).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", BuildConfig.OAUTH_CLIENT_ID)
            .appendQueryParameter("redirect_uri", BuildConfig.OAUTH_REDIRECT_URI)
            .appendQueryParameter("scope", "openid profile email")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("state", state)
            .build()
        withContext(Dispatchers.Main.immediate) {
            CustomTabsIntent.Builder().build().launchUrl(context, authorizeUri)
        }
    }

    suspend fun handleCallback(uri: Uri) {
        require(
            uri.scheme == "vibesync" &&
                uri.host == "auth" &&
                uri.encodedPath == "/callback" &&
                uri.userInfo == null &&
                uri.port == -1 &&
                uri.fragment == null &&
                uri.getQueryParameters("state").size == 1 &&
                uri.getQueryParameters("code").size <= 1 &&
                uri.getQueryParameters("error").size <= 1
        ) {
            "Unexpected or malformed OAuth callback URI."
        }
        val returnedState = uri.getQueryParameter("state")
            ?: throw IllegalStateException("OAuth callback did not contain a state value.")
        val (expectedState, verifier) = withContext(Dispatchers.IO) {
            vault.getSecret(EncryptedVault.OAUTH_PENDING_STATE) to
                vault.getSecret(EncryptedVault.OAUTH_PENDING_VERIFIER)
        }
        if (expectedState.isNullOrBlank() || verifier.isNullOrBlank()) {
            throw IllegalStateException("No matching OAuth login is pending. Start sign-in again.")
        }
        val matches = MessageDigest.isEqual(
            expectedState.toByteArray(Charsets.UTF_8),
            returnedState.toByteArray(Charsets.UTF_8)
        )
        if (!matches) throw SecurityException("OAuth state validation failed.")

        try {
            val callbackError = uri.getQueryParameter("error")
            if (!callbackError.isNullOrBlank()) {
                throw IllegalStateException("OAuth authorization failed: " + callbackError.take(100))
            }
            val code = uri.getQueryParameter("code")?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("OAuth callback did not contain an authorization code.")
            check(isConfigured) { "OAuth token exchange is not configured for this build." }

            val response = httpClient.submitForm(
                url = BuildConfig.OAUTH_TOKEN_ENDPOINT,
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", BuildConfig.OAUTH_REDIRECT_URI)
                    append("client_id", BuildConfig.OAUTH_CLIENT_ID)
                    append("code_verifier", verifier)
                }
            )
            if (!response.status.isSuccess()) {
                throw IllegalStateException("OAuth token exchange failed with HTTP " + response.status.value + ".")
            }
            val accessToken = runCatching {
                Json.parseToJsonElement(response.bodyAsText()).jsonObject["access_token"]?.jsonPrimitive?.content
            }.getOrNull()?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("OAuth token response did not contain an access_token.")
            // Stored separately from provider API keys; the model engine never uses this token.
            withContext(Dispatchers.IO) {
                vault.putSecret(EncryptedVault.OAUTH_ACCESS_TOKEN, accessToken)
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { clearPending() }
        }
    }

    private fun clearPending() {
        vault.removeSecret(EncryptedVault.OAUTH_PENDING_STATE)
        vault.removeSecret(EncryptedVault.OAUTH_PENDING_VERIFIER)
    }

    private fun isHttpsEndpoint(value: String): Boolean =
        runCatching { Uri.parse(value).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false)
}
