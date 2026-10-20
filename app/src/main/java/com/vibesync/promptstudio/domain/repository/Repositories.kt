package com.vibesync.promptstudio.domain.repository

import com.vibesync.promptstudio.domain.model.ModelConfig
import com.vibesync.promptstudio.domain.model.PromptEntity
import com.vibesync.promptstudio.domain.model.PromptRequest
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.domain.model.RunHistory
import kotlinx.coroutines.flow.Flow

interface PromptRepository {
    fun execute(request: PromptRequest): Flow<String>
    suspend fun saveTemplate(prompt: PromptEntity)
    fun observeTemplates(): Flow<List<PromptEntity>>
    suspend fun recordRun(run: RunHistory)
    fun observeHistory(): Flow<List<RunHistory>>
}

interface CredentialRepository {
    suspend fun saveApiKey(provider: ProviderType, apiKey: String)
    suspend fun getApiKey(provider: ProviderType): String?
    suspend fun deleteApiKey(provider: ProviderType)
    suspend fun hasApiKey(provider: ProviderType): Boolean
    suspend fun saveOAuthAccessToken(token: String)
    suspend fun clearOAuthAccessToken()
    suspend fun vaultStatus(): VaultStatus
}

data class VaultStatus(
    val initialized: Boolean,
    val hardwareBacked: Boolean?,
    val message: String
)
