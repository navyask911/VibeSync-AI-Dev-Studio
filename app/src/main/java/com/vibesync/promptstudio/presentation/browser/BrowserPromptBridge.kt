package com.vibesync.promptstudio.presentation.browser

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class PendingBrowserPrompt(val id: String, val text: String)

data class PendingBrowserUrl(val id: String, val url: String)

object BrowserPromptBridge {
    private val mutablePending = MutableStateFlow<PendingBrowserPrompt?>(null)
    val pending: StateFlow<PendingBrowserPrompt?> = mutablePending.asStateFlow()

    private val mutablePendingUrl = MutableStateFlow<PendingBrowserUrl?>(null)
    val pendingUrl: StateFlow<PendingBrowserUrl?> = mutablePendingUrl.asStateFlow()

    fun enqueue(text: String) {
        if (text.isNotBlank()) mutablePending.value = PendingBrowserPrompt(UUID.randomUUID().toString(), text)
    }

    fun consume(id: String) {
        if (mutablePending.value?.id == id) mutablePending.value = null
    }

    /** Queue a trusted HTTPS resource URL for the browser screen to open in its own tab. */
    fun enqueueUrl(url: String) {
        val candidate = url.trim()
        if (candidate.startsWith("https://", ignoreCase = true)) {
            mutablePendingUrl.value = PendingBrowserUrl(UUID.randomUUID().toString(), candidate)
        }
    }

    fun consumeUrl(id: String) {
        if (mutablePendingUrl.value?.id == id) mutablePendingUrl.value = null
    }
}
