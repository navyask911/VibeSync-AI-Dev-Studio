package com.vibesync.promptstudio.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.DiagnosticsStore
import java.security.GeneralSecurityException
import java.security.InvalidKeyException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts each value with AES/GCM using a non-exportable Android Keystore key.
 * Secret names are authenticated as AAD; IVs are randomized for every encryption.
 * A missing/unusable original key never triggers silent replacement while ciphertext remains.
 */
class EncryptedVault(
    context: Context,
    preferencesName: String = DEFAULT_PREFERENCES_NAME,
    private val keyAlias: String = DEFAULT_KEY_ALIAS,
    private val diagnostics: DiagnosticsStore? = null
) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    @Synchronized
    fun putSecret(name: String, value: String) {
        require(name.isNotBlank()) { "Secret name must not be blank." }
        val key = getOrCreateKey()
        val encrypted = try {
            encrypt(name, value.toByteArray(Charsets.UTF_8), key)
        } catch (error: Exception) {
            val failure = classifyKeyError("Could not encrypt this credential with Android Keystore.", error)
            diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "save_failed", "Secure credential save failed: " + failure.failure.name)
            throw failure
        }
        if (!preferences.edit().putString(name, Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "save_failed", "Encrypted credential could not be persisted.")
            throw VaultException("Could not persist the encrypted credential.", VaultFailure.STORAGE_FAILURE)
        }
        diagnostics?.record(DiagnosticSeverity.INFO, "vault", "credential", "saved", "Encrypted credential saved; value and entry name omitted.")
    }

    @Synchronized
    fun getSecret(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        val raw = try {
            Base64.decode(encoded, Base64.NO_WRAP)
        } catch (error: IllegalArgumentException) {
            diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Encrypted credential is malformed; no plaintext was returned.")
            throw VaultException("The encrypted credential is malformed. Re-enter this credential in Settings.", VaultFailure.CIPHERTEXT_CORRUPT, error)
        }
        val key = getExistingKey() ?: run {
            val failure = VaultException(
                "The Android Keystore key for saved credentials is missing. Existing ciphertext cannot be recovered; explicitly reset the vault and re-enter your API keys.",
                VaultFailure.KEY_MISSING,
                recoveryRequired = true
            )
            diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Original Android Keystore key is missing; saved ciphertext was preserved.")
            throw failure
        }
        return try {
            String(decrypt(name, raw, key), Charsets.UTF_8).also {
                diagnostics?.record(DiagnosticSeverity.DEBUG, "vault", "credential", "read", "Encrypted credential read successfully; value and entry name omitted.")
            }
        } catch (error: Exception) {
            when (error) {
                is AEADBadTagException -> {
                    diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Encrypted credential authentication failed.")
                    throw VaultException(
                        "Credential authentication failed. The ciphertext may be damaged or bound to a different entry; the value was not returned.",
                        VaultFailure.AUTHENTICATION_FAILED,
                        error
                    )
                }
                is KeyPermanentlyInvalidatedException, is InvalidKeyException -> {
                    diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Android Keystore key is invalidated or unusable.")
                    throw VaultException(
                        "The Android Keystore key is invalidated or unusable. Existing encrypted credentials cannot be recovered; reset the vault explicitly and re-enter API keys.",
                        VaultFailure.KEY_INVALIDATED,
                        error,
                        recoveryRequired = true
                    )
                }
                is IllegalArgumentException -> {
                    diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Encrypted credential ciphertext is truncated or malformed.")
                    throw VaultException(
                        "The encrypted credential is truncated or malformed. Re-enter this credential in Settings.",
                        VaultFailure.CIPHERTEXT_CORRUPT,
                        error
                    )
                }
                is VaultException -> throw error
                else -> {
                    diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "read_failed", "Credential decryption failed: " + safeClass(error))
                    throw VaultException(
                        "Credential decryption failed due to a cryptographic provider or Keystore error. No plaintext fallback was attempted.",
                        VaultFailure.CRYPTOGRAPHIC_PROVIDER_FAILURE,
                        error
                    )
                }
            }
        }
    }

    @Synchronized
    fun removeSecret(name: String) {
        if (!preferences.edit().remove(name).commit()) {
            diagnostics?.record(DiagnosticSeverity.ERROR, "vault", "credential", "remove_failed", "Encrypted credential could not be removed.")
            throw VaultException("Could not remove the stored credential.", VaultFailure.STORAGE_FAILURE)
        }
        diagnostics?.record(DiagnosticSeverity.INFO, "vault", "credential", "removed", "Stored credential removed; entry name omitted.")
    }

    @Synchronized
    fun keyStatus(): VaultStatusSnapshot {
        val key = try {
            getExistingKey()
        } catch (error: Exception) {
            return VaultStatusSnapshot(
                initialized = false,
                hardwareBacked = null,
                message = "Android Keystore could not open the saved credential key: " + safeClass(error) + ".",
                recoveryRequired = preferences.all.isNotEmpty() && isKeyInvalidated(error)
            )
        }

        val usableKey = if (key == null) {
            if (preferences.all.isNotEmpty()) {
                return VaultStatusSnapshot(
                    initialized = false,
                    hardwareBacked = null,
                    message = "The Android Keystore key is missing while encrypted entries remain. They cannot be recovered by creating a new key; explicitly reset the vault and re-enter credentials.",
                    recoveryRequired = true
                )
            }
            try {
                getOrCreateKey()
            } catch (error: Exception) {
                return VaultStatusSnapshot(
                    initialized = false,
                    hardwareBacked = null,
                    message = "Android Keystore is unavailable for secure credential storage: " + safeClass(error) + ".",
                    recoveryRequired = false
                )
            }
        } else {
            key
        }

        try {
            verifyEncryptionAvailable(usableKey)
        } catch (error: Exception) {
            val invalidated = isKeyInvalidated(error)
            return VaultStatusSnapshot(
                initialized = false,
                hardwareBacked = null,
                message = if (invalidated) {
                    "The Android Keystore key is invalidated or unusable. Existing ciphertext cannot be recovered; explicitly reset the vault and re-enter credentials."
                } else {
                    "The Android Keystore key exists but an AES-GCM operation failed (" + safeClass(error) + "). Secure storage is unavailable; no weaker fallback was enabled."
                },
                recoveryRequired = invalidated && preferences.all.isNotEmpty()
            )
        }

        // AES keys are symmetric SecretKeys: Android's supported metadata factory is SecretKeyFactory.
        // A metadata-provider failure must not be reported as encryption failure if AES-GCM still works.
        val metadata = try {
            val keyInfo = SecretKeyFactory.getInstance(usableKey.algorithm, ANDROID_KEYSTORE)
                .getKeySpec(usableKey, KeyInfo::class.java) as KeyInfo
            keyInfo.isInsideSecureHardware
        } catch (_: Exception) {
            return VaultStatusSnapshot(
                initialized = true,
                hardwareBacked = null,
                message = "AES-GCM encryption with Android Keystore is working, but hardware-backing metadata is unavailable on this device.",
                recoveryRequired = false
            )
        }

        return VaultStatusSnapshot(
            initialized = true,
            hardwareBacked = metadata,
            message = if (metadata) {
                "Android Keystore AES-GCM encryption is working; this device reports hardware-backed key storage."
            } else {
                "Android Keystore AES-GCM encryption is working; this device reports software-backed key storage."
            },
            recoveryRequired = false
        )
    }

    /**
     * This destructive recovery is only allowed when the original key is missing or invalidated.
     * The caller must obtain explicit user confirmation first. All encrypted entries are removed.
     */
    @Synchronized
    fun resetVaultAfterKeyLoss() {
        val status = keyStatus()
        check(status.recoveryRequired) {
            "Vault reset is only available when the original Android Keystore key is missing or invalidated."
        }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            try {
                keyStore.deleteEntry(keyAlias)
            } catch (error: Exception) {
                throw VaultException("Could not reset the invalidated Android Keystore key. Stored ciphertext was left untouched.", VaultFailure.KEYSTORE_UNAVAILABLE, error)
            }
        }
        if (!preferences.edit().clear().commit()) {
            throw VaultException("Could not clear encrypted vault entries. Recheck storage and try again.", VaultFailure.STORAGE_FAILURE)
        }
    }

    private fun encrypt(name: String, plainText: ByteArray, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.iv + cipher.doFinal(plainText)
    }

    private fun decrypt(name: String, combined: ByteArray, key: SecretKey): ByteArray {
        require(combined.size >= IV_LENGTH + TAG_LENGTH_BYTES) { "Ciphertext is truncated." }
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, iv))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }

    @Synchronized
    private fun getExistingKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        getExistingKey()?.let { return it }
        if (preferences.all.isNotEmpty()) {
            throw VaultException(
                "The original Android Keystore key is missing while encrypted entries remain. Reset the vault explicitly before adding new credentials.",
                VaultFailure.KEY_MISSING,
                recoveryRequired = true
            )
        }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun verifyEncryptionAvailable(key: SecretKey) {
        val input = byteArrayOf(0x56, 0x53, 0x31)
        val encryptor = Cipher.getInstance(TRANSFORMATION)
        encryptor.init(Cipher.ENCRYPT_MODE, key)
        encryptor.updateAAD(STATUS_CHECK_AAD)
        val ciphertext = encryptor.doFinal(input)
        val decryptor = Cipher.getInstance(TRANSFORMATION)
        decryptor.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(TAG_LENGTH_BITS, encryptor.iv))
        decryptor.updateAAD(STATUS_CHECK_AAD)
        check(decryptor.doFinal(ciphertext).contentEquals(input)) { "AES-GCM status self-check failed." }
    }

    private fun classifyKeyError(message: String, error: Exception): VaultException = when {
        isKeyInvalidated(error) -> VaultException(
            "The Android Keystore key is invalidated or unusable. Reset the vault explicitly and re-enter API keys.",
            VaultFailure.KEY_INVALIDATED,
            error,
            recoveryRequired = preferences.all.isNotEmpty()
        )
        error is GeneralSecurityException -> VaultException(
            message + " No plaintext or weaker-cipher fallback was attempted.",
            VaultFailure.KEYSTORE_UNAVAILABLE,
            error
        )
        else -> VaultException(message + " " + safeClass(error), VaultFailure.CRYPTOGRAPHIC_PROVIDER_FAILURE, error)
    }

    private fun isKeyInvalidated(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is KeyPermanentlyInvalidatedException || current is InvalidKeyException) return true
            current = current.cause
        }
        return false
    }

    private fun safeClass(error: Throwable): String = error.javaClass.simpleName.take(100).ifBlank { "unknown error" }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEFAULT_KEY_ALIAS = "vibesync_prompt_vault_aes_gcm_v1"
        private const val DEFAULT_PREFERENCES_NAME = "vibesync_encrypted_vault_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8
        private val STATUS_CHECK_AAD = "vibesync-keystore-status".toByteArray(Charsets.UTF_8)

        fun apiKeyName(provider: String) = "api_key_" + provider
        const val OAUTH_ACCESS_TOKEN = "oauth_identity_access_token"
        const val OAUTH_PENDING_STATE = "oauth_pending_state"
        const val OAUTH_PENDING_VERIFIER = "oauth_pending_verifier"
    }
}

data class VaultStatusSnapshot(
    val initialized: Boolean,
    val hardwareBacked: Boolean?,
    val message: String,
    val recoveryRequired: Boolean
)

enum class VaultFailure {
    KEYSTORE_UNAVAILABLE,
    KEY_MISSING,
    KEY_INVALIDATED,
    CIPHERTEXT_CORRUPT,
    AUTHENTICATION_FAILED,
    CRYPTOGRAPHIC_PROVIDER_FAILURE,
    STORAGE_FAILURE
}

class VaultException(
    message: String,
    val failure: VaultFailure,
    cause: Throwable? = null,
    val recoveryRequired: Boolean = false
) : Exception(message, cause)
