package com.vibesync.promptstudio.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vibesync.promptstudio.domain.model.ModelConfig
import com.vibesync.promptstudio.domain.model.PromptEntity
import com.vibesync.promptstudio.domain.model.PromptFailure
import com.vibesync.promptstudio.domain.model.PromptRequest
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.domain.model.RunHistory
import com.vibesync.promptstudio.domain.model.TokenEstimate
import com.vibesync.promptstudio.domain.repository.PromptRepository
import com.vibesync.promptstudio.domain.usecase.ExecutePromptUseCase
import com.vibesync.promptstudio.domain.usecase.SubstituteVariablesUseCase
import com.vibesync.promptstudio.domain.usecase.TokenCounterUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ProviderKeyHealth { UNKNOWN, PRESENT, MISSING, ERROR }

data class PromptStudioUiState(
    val title: String = "New prompt",
    val systemPrompt: String = "You are a precise, helpful assistant. Follow the user's instructions and state uncertainty clearly.",
    val userPrompt: String = "Explain {{topic}} for {{audience}} in three concise bullet points.",
    val variablesText: String = "topic=secure Android app architecture\naudience=junior developers",
    val provider: ProviderType = ProviderType.OPENAI,
    val modelId: String = ProviderType.OPENAI.defaultModel,
    val availableModels: List<String> = emptyList(),
    val isLoadingModels: Boolean = false,
    val providerKeyHealth: Map<ProviderType, ProviderKeyHealth> = emptyMap(),
    val lastFailedProvider: ProviderType? = null,
    val inputRateText: String = "",
    val outputRateText: String = "",
    val response: String = "",
    val isStreaming: Boolean = false,
    val error: String? = null,
    val unresolvedVariables: Set<String> = emptySet(),
    val estimate: TokenEstimate = TokenEstimate(0, 0, null, null),
    val savedTemplates: List<PromptEntity> = emptyList(),
    val recentRuns: List<RunHistory> = emptyList(),
    val successfulRunCount: Int = 0
) {
    val canRun: Boolean get() = !isStreaming && userPrompt.isNotBlank() && unresolvedVariables.isEmpty()
}

class PromptStudioViewModel(
    private val repository: PromptRepository,
    private val executePrompt: ExecutePromptUseCase,
    private val substituteVariables: SubstituteVariablesUseCase,
    private val tokenCounter: TokenCounterUseCase
) : ViewModel() {
    private val mutableState = MutableStateFlow(PromptStudioUiState())
    val state: StateFlow<PromptStudioUiState> = mutableState.asStateFlow()
    private var streamJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeTemplates().collect { prompts ->
                mutableState.update { it.copy(savedTemplates = prompts) }
            }
        }
        viewModelScope.launch {
            repository.observeHistory().collect { runs ->
                mutableState.update { it.copy(recentRuns = runs) }
            }
        }
        recalculate()
        refreshProviderKeyHealth()
    }

    fun updateTitle(value: String) { mutableState.update { it.copy(title = value.take(120)) } }
    fun updateSystemPrompt(value: String) { mutableState.update { it.copy(systemPrompt = value) }; recalculate() }
    fun updateUserPrompt(value: String) { mutableState.update { it.copy(userPrompt = value) }; recalculate() }
    fun updateVariables(value: String) { mutableState.update { it.copy(variablesText = value) }; recalculate() }
    fun updateInputRate(value: String) { mutableState.update { it.copy(inputRateText = value) }; recalculate() }
    fun updateOutputRate(value: String) { mutableState.update { it.copy(outputRateText = value) }; recalculate() }

    fun updateProvider(provider: ProviderType) {
        mutableState.update {
            it.copy(provider = provider, modelId = provider.defaultModel, availableModels = emptyList(), isLoadingModels = false)
        }
        recalculate()
    }

    fun refreshProviderKeyHealth() {
        viewModelScope.launch {
            val health = ProviderType.entries.associateWith { provider ->
                runCatching { repository.hasApiKey(provider) }.fold(
                    onSuccess = { present -> if (present) ProviderKeyHealth.PRESENT else ProviderKeyHealth.MISSING },
                    onFailure = { ProviderKeyHealth.ERROR }
                )
            }
            mutableState.update { it.copy(providerKeyHealth = health) }
        }
    }

    fun refreshModels() {
        val provider = mutableState.value.provider
        viewModelScope.launch {
            mutableState.update { it.copy(isLoadingModels = true) }
            val discovered = runCatching { repository.listModels(provider) }.getOrDefault(emptyList())
            mutableState.update { current ->
                if (current.provider != provider) current
                else current.copy(
                    availableModels = discovered.ifEmpty { provider.modelOptions },
                    isLoadingModels = false
                )
            }
        }
    }

    fun updateModel(value: String) {
        mutableState.update { it.copy(modelId = value.take(120)) }
        recalculate()
    }

    fun loadTemplate(prompt: PromptEntity) {
        mutableState.update {
            it.copy(
                title = prompt.title,
                systemPrompt = prompt.systemPrompt,
                userPrompt = prompt.userPrompt,
                variablesText = substituteVariables.formatVariableLines(prompt.variables),
                response = "",
                availableModels = emptyList(),
                error = null
            )
        }
        recalculate()
    }

    fun saveTemplate() {
        val snapshot = mutableState.value
        viewModelScope.launch {
            runCatching {
                repository.saveTemplate(
                    PromptEntity(
                        title = snapshot.title.ifBlank { "Untitled prompt" },
                        systemPrompt = snapshot.systemPrompt,
                        userPrompt = snapshot.userPrompt,
                        variables = substituteVariables.parseVariableLines(snapshot.variablesText)
                    )
                )
            }.onFailure { error ->
                mutableState.update { it.copy(error = error.message ?: "Could not save prompt.") }
            }
        }
    }

    fun runPrompt() {
        val snapshot = mutableState.value
        if (snapshot.isStreaming) return
        val variables = substituteVariables.parseVariableLines(snapshot.variablesText)
        val system = substituteVariables(snapshot.systemPrompt, variables)
        val user = substituteVariables(snapshot.userPrompt, variables)
        val unresolved = system.unresolvedVariables + user.unresolvedVariables
        if (unresolved.isNotEmpty()) {
            mutableState.update {
                it.copy(unresolvedVariables = unresolved, error = "Resolve the variables listed below before running.")
            }
            return
        }
        val config = configFrom(snapshot)
        val request = PromptRequest(config, system.text, user.text)
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            mutableState.update { it.copy(isStreaming = true, response = "", error = null, lastFailedProvider = null) }
            val responseBuffer = StringBuilder()
            var completedSuccessfully = false
            try {
                executePrompt(request).collect { chunk ->
                    responseBuffer.append(chunk)
                    mutableState.update { current ->
                        current.copy(response = responseBuffer.toString(), estimate = tokenCounter.estimate(
                            system.text + "\n" + user.text, responseBuffer.toString(), config
                        ))
                    }
                }
                repository.recordRun(
                    RunHistory(
                        provider = config.provider,
                        modelId = config.modelId,
                        promptTitle = snapshot.title.ifBlank { "Untitled prompt" },
                        inputText = system.text + "\n\n" + user.text,
                        responseText = responseBuffer.toString()
                    )
                )
                completedSuccessfully = true
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Throwable) {
                mutableState.update {
                    it.copy(error = when (error) {
                        is PromptFailure -> error.message
                        else -> error.message ?: "The request failed unexpectedly."
                    }, lastFailedProvider = snapshot.provider)
                }
            } finally {
                mutableState.update {
                    it.copy(
                        isStreaming = false,
                        successfulRunCount = it.successfulRunCount + if (completedSuccessfully) 1 else 0,
                        estimate = tokenCounter.estimate(
                            system.text + "\n" + user.text, responseBuffer.toString(), config
                        )
                    )
                }
            }
        }
    }

    fun cancelRun() {
        streamJob?.cancel()
        mutableState.update { it.copy(isStreaming = false) }
    }

    private fun configFrom(snapshot: PromptStudioUiState): ModelConfig = ModelConfig(
        provider = snapshot.provider,
        modelId = snapshot.modelId.trim(),
        inputUsdPerMillionTokens = snapshot.inputRateText.toDoubleOrNull()?.takeIf { it >= 0.0 },
        outputUsdPerMillionTokens = snapshot.outputRateText.toDoubleOrNull()?.takeIf { it >= 0.0 },
        maxOutputTokens = 2048
    )

    private fun recalculate() {
        val snapshot = mutableState.value
        val variables = substituteVariables.parseVariableLines(snapshot.variablesText)
        val system = substituteVariables(snapshot.systemPrompt, variables)
        val user = substituteVariables(snapshot.userPrompt, variables)
        val unresolved = system.unresolvedVariables + user.unresolvedVariables
        val config = configFrom(snapshot)
        mutableState.update {
            it.copy(
                unresolvedVariables = unresolved,
                estimate = tokenCounter.estimate(system.text + "\n" + user.text, it.response, config)
            )
        }
    }
}
