package com.vibesync.promptstudio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.domain.repository.VaultStatus
import com.vibesync.promptstudio.domain.usecase.ManageCredentialsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val selectedProvider: ProviderType = ProviderType.OPENAI,
    val secretInput: String = "",
    val savedProviders: Set<ProviderType> = emptySet(),
    val vaultStatus: VaultStatus = VaultStatus(false, null, "Checking Android Keystore…"),
    val message: String? = null,
    val busy: Boolean = false,
    val oauthConfigured: Boolean = false
)

class SettingsViewModel(
    private val manageCredentials: ManageCredentialsUseCase,
    oauthConfigured: Boolean
) : ViewModel() {
    private val mutableState = MutableStateFlow(SettingsUiState(oauthConfigured = oauthConfigured))
    val state: StateFlow<SettingsUiState> = mutableState.asStateFlow()

    init { refresh() }

    fun selectProvider(provider: ProviderType) {
        mutableState.update { it.copy(selectedProvider = provider, secretInput = "", message = null) }
    }

    fun updateSecret(value: String) {
        mutableState.update { it.copy(secretInput = value, message = null) }
    }

    fun refresh() {
        viewModelScope.launch {
            val saved = ProviderType.entries.filter { provider ->
                runCatching { manageCredentials.hasApiKey(provider) }.getOrDefault(false)
            }.toSet()
            val status = runCatching { manageCredentials.vaultStatus() }
                .getOrElse { VaultStatus(false, null, it.message ?: "Keystore check failed.") }
            mutableState.update { it.copy(savedProviders = saved, vaultStatus = status) }
        }
    }

    fun saveKey() {
        val snapshot = mutableState.value
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { manageCredentials.saveApiKey(snapshot.selectedProvider, snapshot.secretInput) }
                .onSuccess {
                    mutableState.update { it.copy(secretInput = "", message = "API key saved in the encrypted vault.") }
                    refresh()
                }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: "Could not save API key.") }
                }
            mutableState.update { it.copy(busy = false) }
        }
    }

    fun deleteKey() {
        val provider = mutableState.value.selectedProvider
        viewModelScope.launch {
            runCatching { manageCredentials.removeApiKey(provider) }
                .onSuccess { mutableState.update { it.copy(secretInput = "", message = "API key removed.") }; refresh() }
                .onFailure { error -> mutableState.update { it.copy(message = error.message ?: "Could not remove API key.") } }
        }
    }

    fun resetVaultAfterKeyLoss() {
        viewModelScope.launch {
            mutableState.update { it.copy(busy = true, message = null) }
            runCatching { manageCredentials.resetVaultAfterKeyLoss() }
                .onSuccess {
                    mutableState.update { it.copy(secretInput = "", message = "Vault reset. Re-enter each provider API key in Settings.") }
                    refresh()
                }
                .onFailure { error ->
                    mutableState.update { it.copy(message = error.message ?: "Vault reset failed; encrypted data was not intentionally discarded.") }
                }
            mutableState.update { it.copy(busy = false) }
        }
    }
}
