package com.vibesync.promptstudio.data.security

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyFactory
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Values are AES/GCM encrypted before being written to preferences. The AES key is generated
 * in AndroidKeyStore and is not exportable. Whether it is hardware-backed depends on the device.
 * The preference name is authenticated as additional data to prevent encrypted values being swapped.
 */
class EncryptedVault(context: Context) {
    private val preferences: SharedPreferences =
        context.applicationContext.getSharedPreferences("vibesync_encrypted_vault_v1", Context.MODE_PRIVATE)

    @Synchronized
    fun putSecret(name: String, value: String) {
        require(name.isNotBlank()) { "Secret name must not be blank." }
        val encrypted = encrypt(name, value.toByteArray(Charsets.UTF_8))
        if (!preferences.edit().putString(name, Base64.encodeToString(encrypted, Base64.NO_WRAP)).commit()) {
            throw VaultException("Could not persist the encrypted credential.")
        }
    }

    @Synchronized
    fun getSecret(name: String): String? {
        val encoded = preferences.getString(name, null) ?: return null
        return try {
            val raw = Base64.decode(encoded, Base64.NO_WRAP)
            String(decrypt(name, raw), Charsets.UTF_8)
        } catch (error: Exception) {
            throw VaultException("The encrypted credential could not be decrypted. It may need to be saved again.", error)
        }
    }

    @Synchronized
    fun removeSecret(name: String) {
        if (!preferences.edit().remove(name).commit()) throw VaultException("Could not remove the stored credential.")
    }

    fun keyStatus(): Pair<Boolean, String> {
        return try {
            val key = getOrCreateKey()
            val keyInfo = KeyFactory.getInstance(key.algorithm, ANDROID_KEYSTORE).getKeySpec(key, KeyInfo::class.java)
            true to if (keyInfo.isInsideSecureHardware) {
                "Android Keystore encryption initialized; key is hardware-backed on this device."
            } else {
                "Android Keystore encryption initialized; this device reports software-backed key storage."
            }
        } catch (error: Exception) {
            false to "Android Keystore is unavailable: " + (error.message ?: error.javaClass.simpleName)
        }
    }

    private fun encrypt(name: String, plainText: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.iv + cipher.doFinal(plainText)
    }

    private fun decrypt(name: String, combined: ByteArray): ByteArray {
        require(combined.size > IV_LENGTH + TAG_LENGTH_BYTES) { "Ciphertext is truncated." }
        val iv = combined.copyOfRange(0, IV_LENGTH)
        val ciphertext = combined.copyOfRange(IV_LENGTH, combined.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        cipher.updateAAD(name.toByteArray(Charsets.UTF_8))
        return cipher.doFinal(ciphertext)
    }

    @Synchronized
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
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

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "vibesync_prompt_vault_aes_gcm_v1"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_LENGTH = 12
        private const val TAG_LENGTH_BITS = 128
        private const val TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8

        fun apiKeyName(provider: String) = "api_key_" + provider
        const val OAUTH_ACCESS_TOKEN = "oauth_identity_access_token"
        const val OAUTH_PENDING_STATE = "oauth_pending_state"
        const val OAUTH_PENDING_VERIFIER = "oauth_pending_verifier"
    }
}

class VaultException(message: String, cause: Throwable? = null) : Exception(message, cause)
