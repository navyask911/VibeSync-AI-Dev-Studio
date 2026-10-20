package com.vibesync.promptstudio.data.repository

import com.vibesync.promptstudio.data.local.PromptDao
import com.vibesync.promptstudio.data.local.PromptTemplateRow
import com.vibesync.promptstudio.data.local.RunHistoryRow
import com.vibesync.promptstudio.data.local.VariableSetRow
import com.vibesync.promptstudio.data.remote.KtorProviderStreamingClient
import com.vibesync.promptstudio.data.security.EncryptedVault
import com.vibesync.promptstudio.domain.model.PromptEntity
import com.vibesync.promptstudio.domain.model.PromptFailure
import com.vibesync.promptstudio.domain.model.PromptRequest
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.domain.model.RunHistory
import com.vibesync.promptstudio.domain.repository.CredentialRepository
import com.vibesync.promptstudio.domain.repository.PromptRepository
import com.vibesync.promptstudio.domain.repository.VaultStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PromptRepositoryImpl(
    private val dao: PromptDao,
    private val vault: EncryptedVault,
    private val streamingClient: KtorProviderStreamingClient
) : PromptRepository {
    private val json = Json { ignoreUnknownKeys = true }

    override fun execute(request: PromptRequest): Flow<String> = flow {
        val key = withContext(Dispatchers.IO) {
            try {
                vault.getSecret(EncryptedVault.apiKeyName(request.config.provider.name))
            } catch (error: Exception) {
                throw PromptFailure.ProviderError("Unable to read the encrypted API key. Save it again in Settings.", error)
            } ?: throw PromptFailure.MissingCredential(request.config.provider)
        }
        emitAll(streamingClient.stream(request, key))
    }

    override suspend fun saveTemplate(prompt: PromptEntity) = withContext(Dispatchers.IO) {
        if (prompt.title.isBlank()) throw PromptFailure.InvalidInput("Give the template a title before saving.")
        val title = prompt.title.trim().take(120)
        val valuesJson = json.encodeToString(prompt.variables)
        dao.upsertPrompt(
            PromptTemplateRow(
                id = prompt.id,
                title = title,
                systemPrompt = prompt.systemPrompt,
                userPrompt = prompt.userPrompt,
                variablesJson = valuesJson,
                updatedAt = prompt.updatedAt
            )
        )
        dao.upsertVariableSet(
            VariableSetRow(
                id = prompt.id,
                name = title,
                valuesJson = valuesJson,
                updatedAt = prompt.updatedAt
            )
        )
    }

    override fun observeTemplates(): Flow<List<PromptEntity>> = dao.observePrompts().map { rows ->
        rows.map { row ->
            PromptEntity(
                id = row.id,
                title = row.title,
                systemPrompt = row.systemPrompt,
                userPrompt = row.userPrompt,
                variables = runCatching { json.decodeFromString<Map<String, String>>(row.variablesJson) }.getOrDefault(emptyMap()),
                updatedAt = row.updatedAt
            )
        }
    }

    override suspend fun recordRun(run: RunHistory) {
        dao.insertRun(
            RunHistoryRow(
                id = run.id,
                provider = run.provider.name,
                modelId = run.modelId,
                promptTitle = run.promptTitle,
                inputText = run.inputText,
                responseText = run.responseText,
                createdAt = run.createdAt
            )
        )
    }

    override fun observeHistory(): Flow<List<RunHistory>> = dao.observeRecentRuns().map { rows ->
        rows.mapNotNull { row ->
            val provider = runCatching { ProviderType.valueOf(row.provider) }.getOrNull() ?: return@mapNotNull null
            RunHistory(
                id = row.id,
                provider = provider,
                modelId = row.modelId,
                promptTitle = row.promptTitle,
                inputText = row.inputText,
                responseText = row.responseText,
                createdAt = row.createdAt
            )
        }
    }
}

class CredentialRepositoryImpl(private val vault: EncryptedVault) : CredentialRepository {
    override suspend fun saveApiKey(provider: ProviderType, apiKey: String) = withContext(Dispatchers.IO) {
        vault.putSecret(EncryptedVault.apiKeyName(provider.name), apiKey)
    }

    override suspend fun getApiKey(provider: ProviderType): String? = withContext(Dispatchers.IO) {
        vault.getSecret(EncryptedVault.apiKeyName(provider.name))
    }

    override suspend fun deleteApiKey(provider: ProviderType) = withContext(Dispatchers.IO) {
        vault.removeSecret(EncryptedVault.apiKeyName(provider.name))
    }

    override suspend fun hasApiKey(provider: ProviderType): Boolean = withContext(Dispatchers.IO) {
        !vault.getSecret(EncryptedVault.apiKeyName(provider.name)).isNullOrBlank()
    }

    override suspend fun saveOAuthAccessToken(token: String) = withContext(Dispatchers.IO) {
        require(token.isNotBlank()) { "OAuth access token is blank." }
        vault.putSecret(EncryptedVault.OAUTH_ACCESS_TOKEN, token)
    }

    override suspend fun clearOAuthAccessToken() = withContext(Dispatchers.IO) {
        vault.removeSecret(EncryptedVault.OAUTH_ACCESS_TOKEN)
    }

    override suspend fun vaultStatus(): VaultStatus = withContext(Dispatchers.IO) {
        val (initialized, message) = vault.keyStatus()
        val hardware = if (initialized) message.contains("hardware-backed", ignoreCase = true) else null
        VaultStatus(initialized, hardware, message)
    }
}
