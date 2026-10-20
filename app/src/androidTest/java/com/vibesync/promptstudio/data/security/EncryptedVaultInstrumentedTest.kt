package com.vibesync.promptstudio.data.security

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.security.KeyStore
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class EncryptedVaultInstrumentedTest {
    private lateinit var context: Context
    private lateinit var vault: EncryptedVault
    private lateinit var preferencesName: String
    private lateinit var keyAlias: String

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val suffix = UUID.randomUUID().toString().replace("-", "")
        preferencesName = "vibesync_vault_test_" + suffix
        keyAlias = "vibesync_test_key_" + suffix
        vault = EncryptedVault(context, preferencesName, keyAlias)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit().clear().commit()
        runCatching {
            val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
        }
    }

    @Test
    fun encryptsReadsReplacesAndRemovesCredential() {
        vault.putSecret("api_key", "first-secret-value")
        assertEquals("first-secret-value", vault.getSecret("api_key"))

        vault.putSecret("api_key", "replacement-secret-value")
        assertEquals("replacement-secret-value", vault.getSecret("api_key"))

        vault.removeSecret("api_key")
        assertEquals(null, vault.getSecret("api_key"))
    }

    @Test
    fun statusUsesAndroidKeystoreSecretKeyMetadataWithoutDisablingEncryption() {
        vault.putSecret("status_check", "healthy")
        val status = vault.keyStatus()

        assertTrue(status.initialized)
        assertNotNull(status.message)
        assertEquals("healthy", vault.getSecret("status_check"))
    }

    @Test
    fun missingOriginalKeyIsReportedAndNeverSilentlyRecreatedForExistingCiphertext() {
        vault.putSecret("lost_key_item", "cannot-recover-this")
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        keyStore.deleteEntry(keyAlias)

        val failure = assertThrows(VaultException::class.java) {
            vault.getSecret("lost_key_item")
        }
        assertEquals(VaultFailure.KEY_MISSING, failure.failure)
        assertTrue(vault.keyStatus().recoveryRequired)
        assertThrows(VaultException::class.java) {
            vault.putSecret("another_item", "new-secret")
        }
        assertNotNull(context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).getString("lost_key_item", null))
    }

    @Test
    fun malformedCiphertextFailsClosed() {
        vault.putSecret("corrupt", "secret")
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
            .edit().putString("corrupt", "not-base64!").commit()

        val failure = assertThrows(VaultException::class.java) { vault.getSecret("corrupt") }
        assertEquals(VaultFailure.CIPHERTEXT_CORRUPT, failure.failure)
    }

    @Test
    fun movingCiphertextToAnotherEntryFailsAuthenticatedDataCheck() {
        vault.putSecret("entry_a", "secret")
        val prefs = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
        val ciphertext = prefs.getString("entry_a", null)
        prefs.edit().putString("entry_b", ciphertext).commit()

        val failure = assertThrows(VaultException::class.java) { vault.getSecret("entry_b") }
        assertTrue(failure.failure == VaultFailure.AUTHENTICATION_FAILED || failure.failure == VaultFailure.CIPHERTEXT_CORRUPT)
        assertFalse(failure.message.orEmpty().contains("secret"))
    }

    @Test
    fun explicitResetAfterKeyLossDeletesUnrecoverableCiphertextAndAllowsReentry() {
        vault.putSecret("lost", "secret")
        KeyStore.getInstance("AndroidKeyStore").apply {
            load(null)
            deleteEntry(keyAlias)
        }

        assertTrue(vault.keyStatus().recoveryRequired)
        vault.resetVaultAfterKeyLoss()
        assertEquals(null, vault.getSecret("lost"))
        vault.putSecret("reentered", "new-secret")
        assertEquals("new-secret", vault.getSecret("reentered"))
    }
}
