package com.vibesync.promptstudio.data.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom

object PkceUtil {
    private val random = SecureRandom()

    fun generateCodeVerifier(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateCodeChallenge(verifier: String): String {
        require(verifier.length in 43..128) { "PKCE verifier length must be between 43 and 128 characters." }
        require(verifier.all { it.isLetterOrDigit() || it in "-._~" }) { "PKCE verifier contains invalid characters." }
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun generateState(): String {
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
    }
}
