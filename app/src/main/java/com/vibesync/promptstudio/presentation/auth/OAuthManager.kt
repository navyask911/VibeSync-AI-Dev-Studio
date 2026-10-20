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
data class OAuthClientConfiguration(
    val clientId: String,
    val authorizeEndpoint: String,
    val tokenEndpoint: String,
    val redirectUri: String = DEFAULT_REDIRECT_URI,
    val scopes: String = "openid email profile"
) {
    companion object {
        const val DEFAULT_REDIRECT_URI = "vibesync://auth/callback"
    }
}

/**
 * Generic public-client OAuth 2.0 Authorization Code + PKCE plumbing.
 * Identity tokens are stored separately and are never used as provider API keys.
 */
class OAuthManager(
    context: Context,
    private val vault: EncryptedVault,
    private val httpClient: HttpClient
) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("vibesync_oauth_pending", Context.MODE_PRIVATE)

    val isConfigured: Boolean
        get() = BuildConfig.OAUTH_CLIENT_ID.isNotBlank() &&
            isHttpsEndpoint(BuildConfig.OAUTH_AUTHORIZE_ENDPOINT) &&
            isHttpsEndpoint(BuildConfig.OAUTH_TOKEN_ENDPOINT)

    suspend fun launch(context: Context) {
        launch(
            context,
            OAuthClientConfiguration(
                clientId = BuildConfig.OAUTH_CLIENT_ID,
                authorizeEndpoint = BuildConfig.OAUTH_AUTHORIZE_ENDPOINT,
                tokenEndpoint = BuildConfig.OAUTH_TOKEN_ENDPOINT,
                redirectUri = BuildConfig.OAUTH_REDIRECT_URI,
                scopes = "openid profile email"
            )
        )
    }

    suspend fun launch(context: Context, configuration: OAuthClientConfiguration) {
        require(configuration.clientId.isNotBlank()) { "Enter a developer app Client ID." }
        require(isHttpsEndpoint(configuration.authorizeEndpoint)) { "Authorization endpoint must use HTTPS." }
        require(isHttpsEndpoint(configuration.tokenEndpoint)) { "Token endpoint must use HTTPS." }
        require(configuration.redirectUri == OAuthClientConfiguration.DEFAULT_REDIRECT_URI) {
            "This app currently receives OAuth callbacks only at " + OAuthClientConfiguration.DEFAULT_REDIRECT_URI
        }
        require(configuration.scopes.isNotBlank()) { "Enter at least one OAuth scope." }

        val verifier = PkceUtil.generateCodeVerifier()
        val state = PkceUtil.generateState()
        withContext(Dispatchers.IO) {
            vault.putSecret(EncryptedVault.OAUTH_PENDING_VERIFIER, verifier)
            vault.putSecret(EncryptedVault.OAUTH_PENDING_STATE, state)
            prefs.edit().putString(PENDING_CONFIGURATION, encodeConfiguration(configuration)).apply()
        }

        val challenge = PkceUtil.generateCodeChallenge(verifier)
        val authorizeUri = Uri.parse(configuration.authorizeEndpoint).buildUpon()
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("client_id", configuration.clientId)
            .appendQueryParameter("redirect_uri", configuration.redirectUri)
            .appendQueryParameter("scope", configuration.scopes)
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
        val configuration = loadPendingConfiguration()
            ?: throw IllegalStateException("OAuth client configuration was not saved. Start sign-in again.")
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
            require(configuration.clientId.isNotBlank() && isHttpsEndpoint(configuration.tokenEndpoint)) {
                "OAuth token exchange is not configured for this client."
            }

            val response = httpClient.submitForm(
                url = configuration.tokenEndpoint,
                formParameters = Parameters.build {
                    append("grant_type", "authorization_code")
                    append("code", code)
                    append("redirect_uri", configuration.redirectUri)
                    append("client_id", configuration.clientId)
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
        prefs.edit().remove(PENDING_CONFIGURATION).apply()
    }

    private fun encodeConfiguration(configuration: OAuthClientConfiguration): String =
        org.json.JSONObject()
            .put("clientId", configuration.clientId)
            .put("authorizeEndpoint", configuration.authorizeEndpoint)
            .put("tokenEndpoint", configuration.tokenEndpoint)
            .put("redirectUri", configuration.redirectUri)
            .put("scopes", configuration.scopes)
            .toString()

    private fun loadPendingConfiguration(): OAuthClientConfiguration? = runCatching {
        val json = org.json.JSONObject(prefs.getString(PENDING_CONFIGURATION, null) ?: return null)
        OAuthClientConfiguration(
            clientId = json.getString("clientId"),
            authorizeEndpoint = json.getString("authorizeEndpoint"),
            tokenEndpoint = json.getString("tokenEndpoint"),
            redirectUri = json.getString("redirectUri"),
            scopes = json.getString("scopes")
        )
    }.getOrNull()

    private fun isHttpsEndpoint(value: String): Boolean =
        runCatching { Uri.parse(value).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false)

    companion object {
        private const val PENDING_CONFIGURATION = "pending_oauth_configuration"
    }
}
