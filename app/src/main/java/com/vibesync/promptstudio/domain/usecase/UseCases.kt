package com.vibesync.promptstudio.domain.usecase

import com.vibesync.promptstudio.domain.model.ModelConfig
import com.vibesync.promptstudio.domain.model.PromptEntity
import com.vibesync.promptstudio.domain.model.PromptFailure
import com.vibesync.promptstudio.domain.model.PromptRequest
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.domain.model.RunHistory
import com.vibesync.promptstudio.domain.model.SubstitutionResult
import com.vibesync.promptstudio.domain.model.TokenEstimate
import com.vibesync.promptstudio.domain.repository.CredentialRepository
import com.vibesync.promptstudio.domain.repository.PromptRepository
import kotlinx.coroutines.flow.Flow
import java.util.Locale
import javax.inject.Inject
import kotlin.math.ceil

class SubstituteVariablesUseCase @Inject constructor() {
    private val variablePattern = Regex("""\{\{([A-Za-z_][A-Za-z0-9_]*)}}""")

    operator fun invoke(template: String, variables: Map<String, String>): SubstitutionResult {
        val unresolved = linkedSetOf<String>()
        val replaced = variablePattern.replace(template) { match ->
            val name = match.groupValues[1]
            val value = variables[name]
            if (value == null) {
                unresolved += name
                match.value
            } else {
                value
            }
        }
        return SubstitutionResult(replaced, unresolved)
    }

    fun parseVariableLines(raw: String): Map<String, String> {
        val result = linkedMapOf<String, String>()
        raw.lineSequence().forEach { line ->
            val separator = line.indexOf('=')
            if (separator > 0) {
                val key = line.substring(0, separator).trim()
                val value = line.substring(separator + 1).trim()
                if (key.matches(Regex("""[A-Za-z_][A-Za-z0-9_]*"""))) result[key] = value
            }
        }
        return result
    }

    fun formatVariableLines(variables: Map<String, String>): String =
        variables.entries.joinToString("\n") { it.key + "=" + it.value }
}

class TokenCounterUseCase @Inject constructor() {
    // A deliberately approximate cross-provider estimate. Exact counts require provider tokenizers.
    fun count(text: String): Int = if (text.isBlank()) 0 else ceil(text.length / 4.0).toInt().coerceAtLeast(1)

    fun estimate(input: String, output: String, config: ModelConfig): TokenEstimate {
        val inputTokens = count(input)
        val outputTokens = count(output)
        val inCost = config.inputUsdPerMillionTokens?.takeIf { it >= 0.0 }?.let { inputTokens * it / 1_000_000.0 }
        val outCost = config.outputUsdPerMillionTokens?.takeIf { it >= 0.0 }?.let { outputTokens * it / 1_000_000.0 }
        return TokenEstimate(inputTokens, outputTokens, inCost, outCost)
    }

    fun formatUsd(value: Double): String = String.format(Locale.US, "$%.6f", value)
}

class ExecutePromptUseCase @Inject constructor(private val repository: PromptRepository) {
    operator fun invoke(request: PromptRequest): Flow<String> {
        if (request.systemPrompt.isBlank() && request.userPrompt.isBlank()) {
            throw PromptFailure.InvalidInput("Enter a system prompt or user prompt before running.")
        }
        if (request.userPrompt.isBlank()) {
            throw PromptFailure.InvalidInput("The user prompt cannot be empty.")
        }
        if (!request.config.modelId.matches(Regex("""[A-Za-z0-9._:-]{1,120}"""))) {
            throw PromptFailure.InvalidInput("The model identifier contains unsupported characters.")
        }
        if (request.config.maxOutputTokens !in 1..32768) {
            throw PromptFailure.InvalidInput("Maximum output tokens must be between 1 and 32768.")
        }
        return repository.execute(request)
    }
}

class ManageCredentialsUseCase @Inject constructor(private val repository: CredentialRepository) {
    suspend fun saveApiKey(provider: ProviderType, key: String) {
        val sanitized = key.trim()
        if (sanitized.length < 8 || sanitized.any { it.isWhitespace() }) {
            throw IllegalArgumentException("Enter a valid API key without whitespace.")
        }
        repository.saveApiKey(provider, sanitized)
    }

    suspend fun removeApiKey(provider: ProviderType) = repository.deleteApiKey(provider)
    suspend fun hasApiKey(provider: ProviderType) = repository.hasApiKey(provider)
    suspend fun vaultStatus() = repository.vaultStatus()
    suspend fun saveOAuthAccessToken(token: String) = repository.saveOAuthAccessToken(token)
}
