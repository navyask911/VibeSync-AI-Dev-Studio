package com.vibesync.promptstudio.domain.model

import java.util.UUID

enum class ProviderType(
    val displayName: String,
    val defaultModel: String,
    val modelOptions: List<String>
) {
    OPENAI("OpenAI", "gpt-4o", listOf("gpt-4o", "gpt-4o-mini", "gpt-4.1")),
    ANTHROPIC("Anthropic", "claude-sonnet-4-6", listOf("claude-sonnet-4-6", "claude-opus-4-8", "claude-haiku-4-5")),
    GEMINI("Google Gemini", "gemini-3.8-flash", listOf("gemini-3.8-flash", "gemini-2.5-flash", "gemini-2.5-pro"))
}

data class DynamicVariable(val name: String, val value: String)

data class PromptEntity(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val systemPrompt: String,
    val userPrompt: String,
    val variables: Map<String, String> = emptyMap(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ModelConfig(
    val provider: ProviderType = ProviderType.OPENAI,
    val modelId: String = ProviderType.OPENAI.defaultModel,
    val inputUsdPerMillionTokens: Double? = null,
    val outputUsdPerMillionTokens: Double? = null,
    val maxOutputTokens: Int = 2048
)

data class PromptRequest(
    val config: ModelConfig,
    val systemPrompt: String,
    val userPrompt: String
)

data class SubstitutionResult(
    val text: String,
    val unresolvedVariables: Set<String>
)

data class RunHistory(
    val id: String = UUID.randomUUID().toString(),
    val provider: ProviderType,
    val modelId: String,
    val promptTitle: String,
    val inputText: String,
    val responseText: String,
    val createdAt: Long = System.currentTimeMillis()
)

data class TokenEstimate(
    val inputTokens: Int,
    val outputTokens: Int,
    val inputCostUsd: Double?,
    val outputCostUsd: Double?
) {
    val totalCostUsd: Double?
        get() = if (inputCostUsd != null && outputCostUsd != null) inputCostUsd + outputCostUsd else null
}

sealed class PromptFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class InvalidInput(message: String) : PromptFailure(message)
    class MissingCredential(provider: ProviderType) : PromptFailure("Add an API key for " + provider.displayName + " in Settings.")
    class Unauthorized : PromptFailure("The provider rejected this API key (HTTP 401/403). Check the key and its permissions.")
    class RateLimited(val retryAfterSeconds: Long? = null) : PromptFailure(
        if (retryAfterSeconds != null) "Rate limited. Try again in " + retryAfterSeconds + " seconds."
        else "Rate limited by the provider. Wait a moment and try again."
    )
    class Timeout(cause: Throwable? = null) : PromptFailure("The request timed out. Check connectivity and retry.", cause)
    class Network(cause: Throwable? = null) : PromptFailure("Could not reach the provider. Check your network connection.", cause)
    class ProviderError(message: String, cause: Throwable? = null) : PromptFailure(message, cause)
}
