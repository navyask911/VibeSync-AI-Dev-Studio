package com.vibesync.promptstudio.presentation.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PendingBrowserPrompt(val id: String, val text: String)

object BrowserPromptBridge {
    private val mutablePending = MutableStateFlow<PendingBrowserPrompt?>(null)
    val pending: StateFlow<PendingBrowserPrompt?> = mutablePending.asStateFlow()

    fun enqueue(text: String) {
        if (text.isNotBlank()) mutablePending.value = PendingBrowserPrompt(UUID.randomUUID().toString(), text)
    }

    fun consume(id: String) {
        if (mutablePending.value?.id == id) mutablePending.value = null
    }
}
