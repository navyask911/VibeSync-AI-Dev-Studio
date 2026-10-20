package com.vibesync.promptstudio.presentation.browser

import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.lang.ref.WeakReference
import java.util.UUID

data class AiTabItem(
    val id: String = UUID.randomUUID().toString(),
    val provider: AiProvider,
    val url: String = provider.startUrl,
    val title: String = "",
    val loading: Boolean = true,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false
)

class TabManagerViewModel : ViewModel() {
    private val firstTab = AiTabItem(provider = AiProvider.GEMINI)
    private val mutableTabs = MutableStateFlow(listOf(firstTab))
    val tabs: StateFlow<List<AiTabItem>> = mutableTabs.asStateFlow()

    private val mutableActiveTabId = MutableStateFlow(firstTab.id)
    val activeTabId: StateFlow<String> = mutableActiveTabId.asStateFlow()

    // WebViews are created with the current Activity context by AndroidView. Weak references let
    // the screen own attachment/layout while this ViewModel controls tab lifetime and destruction.
    private val webViews = linkedMapOf<String, WeakReference<WebView>>()

    fun openTab(provider: AiProvider, customUrl: String? = null) {
        val startUrl = customUrl?.let(::normalizeHttpUrl) ?: if (customUrl == null) provider.startUrl else null
        if (startUrl == null) return
        val effectiveProvider = if (customUrl != null && AiProvider.fromHost(startUrl) == null) provider else
            AiProvider.fromHost(startUrl) ?: provider
        val tab = AiTabItem(provider = effectiveProvider, url = startUrl)
        mutableTabs.value = mutableTabs.value + tab
        mutableActiveTabId.value = tab.id
    }

    fun switchTab(tabId: String) {
        if (mutableTabs.value.any { it.id == tabId }) mutableActiveTabId.value = tabId
    }

    fun closeTab(tabId: String) {
        destroyTabWebView(tabId)
        val oldTabs = mutableTabs.value
        val index = oldTabs.indexOfFirst { it.id == tabId }
        if (index < 0) return
        val next = oldTabs.filterNot { it.id == tabId }
        mutableTabs.value = next
        if (mutableActiveTabId.value == tabId) {
            mutableActiveTabId.value = next.getOrNull((index - 1).coerceAtLeast(0))?.id.orEmpty()
        }
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val current = mutableTabs.value
        if (fromIndex !in current.indices || toIndex !in current.indices || fromIndex == toIndex) return
        val mutable = current.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(toIndex, item)
        mutableTabs.value = mutable
    }

    fun navigate(tabId: String, rawUrl: String) {
        val url = normalizeHttpUrl(rawUrl) ?: return
        updateTab(tabId) { it.copy(url = url, title = "", loading = true) }
    }

    fun updatePage(tabId: String, url: String, title: String, loading: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        val safeUrl = normalizeHttpUrl(url) ?: return
        updateTab(tabId) {
            it.copy(url = safeUrl, title = title.take(180), loading = loading, canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }

    fun webViewFor(tabId: String): WebView? = webViews[tabId]?.get()

    fun registerWebView(tabId: String, webView: WebView) {
        webViews[tabId] = WeakReference(webView)
    }

    fun registeredWebViewIds(): Set<String> = webViews.keys.toSet()

    fun forgetWebView(tabId: String) {
        webViews.remove(tabId)
    }

    fun goBack() {
        val view = webViewFor(mutableActiveTabId.value)
        if (view?.canGoBack() == true) view.goBack()
    }

    fun goForward() {
        val view = webViewFor(mutableActiveTabId.value)
        if (view?.canGoForward() == true) view.goForward()
    }

    fun reload() {
        webViewFor(mutableActiveTabId.value)?.reload()
    }

    private fun updateTab(tabId: String, transform: (AiTabItem) -> AiTabItem) {
        mutableTabs.value = mutableTabs.value.map { if (it.id == tabId) transform(it) else it }
    }

    private fun destroyTabWebView(tabId: String) {
        val view = webViews.remove(tabId)?.get() ?: return
        runCatching { view.stopLoading() }
        runCatching { (view.parent as? ViewGroup)?.removeView(view) }
        runCatching { view.removeAllViews() }
        runCatching { view.destroy() }
    }

    override fun onCleared() {
        val cleanup = {
            webViews.keys.toList().forEach(::destroyTabWebView)
        }
        if (Looper.myLooper() == Looper.getMainLooper()) cleanup()
        else Handler(Looper.getMainLooper()).post(cleanup)
        super.onCleared()
    }

    private fun normalizeHttpUrl(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty() || trimmed.length > MAX_URL_LENGTH) return null
        val candidate = if ("://" in trimmed) trimmed else "https://$trimmed"
        val uri = runCatching { Uri.parse(candidate) }.getOrNull() ?: return null
        if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.userInfo != null) return null
        return uri.toString()
    }

    companion object {
        private const val MAX_URL_LENGTH = 2048
    }
}
