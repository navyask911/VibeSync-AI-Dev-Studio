package com.vibesync.promptstudio.presentation.browser

import android.content.Context
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vibesync.promptstudio.R
import org.json.JSONObject
import android.webkit.WebResourceError
import android.webkit.WebResourceResponse
import android.webkit.WebView.HitTestResult

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWebStudioScreen(
    onOpenDeveloperApi: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val manager: TabManagerViewModel = viewModel()
    val tabs by manager.tabs.collectAsState()
    val activeId by manager.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId }
    var showProviderSheet by remember { mutableStateOf(false) }
    var showTabOverview by remember { mutableStateOf(false) }
    var browserMenuExpanded by remember { mutableStateOf(false) }
    var address by remember(activeId, activeTab?.url) { mutableStateOf(activeTab?.url.orEmpty()) }
    var prompt by remember { mutableStateOf("") }
    var feedback by remember { mutableStateOf<String?>(null) }
    var hostContainer by remember { mutableStateOf<FrameLayout?>(null) }
    val popupPool = remember { mutableMapOf<String, MutableList<WebView>>() }
    val browserPageFailedText = stringResource(R.string.browser_page_failed)
    val promptSentText = stringResource(R.string.prompt_sent)
    val promptFilledText = stringResource(R.string.prompt_filled_no_send)
    val promptNoInputText = stringResource(R.string.prompt_input_not_found)
    val promptUnsupportedText = stringResource(R.string.prompt_injection_unsupported)

    BackHandler(enabled = activeTab != null) {
        val activeView = activeTab?.let { manager.webViewFor(it.id) }
        if (activeView?.canGoBack() == true) activeView.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text(stringResource(R.string.ai_web_studio), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.browser_mode_label), maxLines = 1, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            val webView = activeTab?.let { manager.webViewFor(it.id) }
                            if (webView?.canGoBack() == true) webView.goBack() else onBack()
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.browser_back)) }
                    },
                    actions = {
                        IconButton(onClick = { manager.goForward() }, enabled = activeTab?.canGoForward == true) { Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.browser_forward)) }
                        IconButton(onClick = { manager.reload() }, enabled = activeTab != null) { Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.browser_reload)) }
                        Box {
                            IconButton(onClick = { browserMenuExpanded = true }) {
                                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.mode_api))
                            }
                            DropdownMenu(expanded = browserMenuExpanded, onDismissRequest = { browserMenuExpanded = false }) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.open_external_browser)) },
                                    onClick = { browserMenuExpanded = false; openExternalBrowser(context, activeTab?.url) },
                                    enabled = activeTab != null
                                )
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.mode_api)) },
                                    onClick = { browserMenuExpanded = false; onOpenDeveloperApi() }
                                )
                            }
                        }
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(start = 8.dp, end = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LazyRow(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(tabs, key = { it.id }) { tab ->
                            BrowserTabChip(
                                tab = tab,
                                selected = tab.id == activeId,
                                onSelect = { manager.switchTab(tab.id) },
                                onClose = { manager.closeTab(tab.id) },
                                onReorder = { from, to -> manager.reorderTabs(from, to) },
                                index = tabs.indexOfFirst { it.id == tab.id },
                                tabCount = tabs.size
                            )
                        }
                    }
                    IconButton(onClick = { showTabOverview = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Tab, contentDescription = stringResource(R.string.tab_overview))
                    }
                    IconButton(onClick = { showProviderSheet = true }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Add, contentDescription = stringResource(R.string.open_provider_directory))
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.address_bar)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    Button(onClick = {
                        if (address.isNotBlank()) {
                            if (activeTab == null) manager.openTab(AiProvider.CUSTOM, address)
                            else manager.navigate(activeTab.id, address)
                            feedback = null
                        }
                    }) { Text(stringResource(R.string.go)) }
                }
            }
        },
        bottomBar = {
            Column(
                modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                feedback?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it; feedback = null },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text(stringResource(R.string.prompt_hint)) },
                        minLines = 1,
                        maxLines = 4
                    )
                    Button(
                        onClick = {
                            val tab = activeTab
                            if (tab == null || !tab.provider.supportsPromptInjection(tab.url)) {
                                feedback = promptUnsupportedText
                            } else if (prompt.isBlank()) {
                                feedback = promptNoInputText
                            } else {
                                val js = makePromptInjectionScript(prompt, tab.provider)
                                manager.webViewFor(tab.id)?.evaluateJavascript(js) { result ->
                                    feedback = when {
                                        result?.contains("no-input") == true -> promptNoInputText
                                        result?.contains("no-send") == true -> promptFilledText
                                        else -> promptSentText
                                    }
                                    if (result?.contains("no-input") != true) prompt = ""
                                }
                            }
                        },
                        enabled = prompt.isNotBlank() && activeTab != null
                    ) { Text(stringResource(R.string.send)) }
                }
                Text(
                    stringResource(R.string.browser_privacy_note),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    ) { innerPadding ->
        Box(Modifier.fillMaxSize().padding(innerPadding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { viewContext ->
                    FrameLayout(viewContext).apply {
                        clipChildren = true
                        clipToPadding = true
                        hostContainer = this
                        syncWebViewPool(this, tabs, activeId, manager, context, popupPool, browserPageFailedText)
                    }
                },
                update = { host ->
                    hostContainer = host
                    syncWebViewPool(host, tabs, activeId, manager, context, popupPool, browserPageFailedText)
                }
            )
            if (activeTab?.loading == true) {
                CircularProgressIndicator(Modifier.align(Alignment.TopCenter).padding(10.dp).size(24.dp))
            }
            if (activeTab == null) {
                Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.no_open_tabs), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showProviderSheet = true }) { Text(stringResource(R.string.open_provider_directory)) }
                }
            }
        }
    }

    if (showProviderSheet) {
        ModalBottomSheet(onDismissRequest = { showProviderSheet = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.provider_directory), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.provider_directory_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().height(420.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AiProvider.entries.filterNot { it == AiProvider.CUSTOM }) { provider ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(38.dp)) {
                                    Box(contentAlignment = Alignment.Center) { Text(provider.icon, fontWeight = FontWeight.Bold) }
                                }
                                Text(stringResource(provider.titleRes), style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                FilledTonalButton(onClick = {
                                    manager.openTab(provider)
                                    showProviderSheet = false
                                    feedback = null
                                }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_tab)) }
                            }
                        }
                    }
                }
                var customUrl by remember { mutableStateOf("") }
                Row(Modifier.fillMaxWidth().padding(bottom = 20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = customUrl, onValueChange = { customUrl = it }, modifier = Modifier.weight(1f), singleLine = true, label = { Text(stringResource(R.string.custom_address)) })
                    Button(onClick = {
                        if (customUrl.isNotBlank()) {
                            manager.openTab(AiProvider.CUSTOM, customUrl)
                            showProviderSheet = false
                        }
                    }, enabled = customUrl.isNotBlank()) { Text(stringResource(R.string.open_tab)) }
                }
            }
        }
    }

    if (showTabOverview) {
        AlertDialog(
            onDismissRequest = { showTabOverview = false },
            title = { Text(stringResource(R.string.open_tabs_title, tabs.size)) },
            text = {
                if (tabs.isEmpty()) Text(stringResource(R.string.no_open_tabs))
                else LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(tabs, key = { it.id }) { tab ->
                        val tabIndex = tabs.indexOfFirst { it.id == tab.id }
                        val density = androidx.compose.ui.platform.LocalDensity.current
                        Card(
                            modifier = Modifier.fillMaxWidth()
                                .pointerInput(tab.id, tabs.size) {
                                    var totalDrag = 0f
                                    detectDragGesturesAfterLongPress(
                                        onDragEnd = {
                                            val delta = (totalDrag / with(density) { 120.dp.toPx() }).toInt()
                                            if (delta != 0) manager.reorderTabs(tabIndex, (tabIndex + delta).coerceIn(0, tabs.lastIndex))
                                        },
                                        onDragCancel = { totalDrag = 0f },
                                        onDrag = { change, amount -> change.consume(); totalDrag += amount.x }
                                    )
                                }
                                .pointerInput(tab.id) {
                                    var drag = 0f
                                    detectHorizontalDragGestures(
                                        onHorizontalDrag = { change, amount -> change.consume(); drag += amount },
                                        onDragEnd = {
                                            if (kotlin.math.abs(drag) > 100.dp.toPx()) manager.closeTab(tab.id)
                                        }
                                    )
                                },
                            colors = CardDefaults.cardColors(containerColor = if (tab.id == activeId) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    val providerLabel = stringResource(tab.provider.titleRes)
                                    Text(tab.title.ifBlank { providerLabel }, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                                    Text(tab.url, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelSmall)
                                }
                                TextButton(onClick = { manager.switchTab(tab.id); showTabOverview = false }) { Text(stringResource(R.string.switch_tab)) }
                                IconButton(onClick = { manager.closeTab(tab.id) }) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_tab)) }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showTabOverview = false }) { Text(stringResource(R.string.done)) } }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // Main tab WebViews remain alive when this destination stays on the back stack.
            hostContainer?.let { host ->
                for (i in 0 until host.childCount) host.getChildAt(i).visibility = View.GONE
            }
            popupPool.values.flatten().forEach { popup ->
                runCatching { (popup.parent as? ViewGroup)?.removeView(popup) }
                runCatching { popup.stopLoading(); popup.removeAllViews(); popup.destroy() }
            }
            popupPool.clear()
        }
    }
}

@Composable
private fun BrowserTabChip(
    tab: AiTabItem,
    selected: Boolean,
    onSelect: () -> Unit,
    onClose: () -> Unit,
    onReorder: (Int, Int) -> Unit,
    index: Int,
    tabCount: Int
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .pointerInput(index, tabCount) {
                var total = 0f
                detectDragGesturesAfterLongPress(
                    onDragEnd = {
                        val deltaSlots = (total / with(density) { 112.dp.toPx() }).toInt()
                        if (deltaSlots != 0) onReorder(index, (index + deltaSlots).coerceIn(0, tabCount - 1))
                    },
                    onDragCancel = { total = 0f },
                    onDrag = { change, amount -> change.consume(); total += amount.x }
                )
            }
            .pointerInput(tab.id) {
                var total = 0f
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount -> change.consume(); total += amount },
                    onDragEnd = { if (kotlin.math.abs(total) > 90.dp.toPx()) onClose() }
                )
            }
            .clip(RoundedCornerShape(18.dp))
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(tab.provider.icon, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
            TextButton(onClick = onSelect, contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 2.dp, vertical = 0.dp)) {
                Text(stringResource(tab.provider.shortTitleRes), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.labelMedium)
            }
            IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_tab), modifier = Modifier.size(16.dp))
            }
        }
    }
}

private fun syncWebViewPool(
    host: FrameLayout,
    tabs: List<AiTabItem>,
    activeId: String,
    manager: TabManagerViewModel,
    context: android.content.Context,
    popupPool: MutableMap<String, MutableList<WebView>>,
    browserPageFailedText: String
) {
    val ids = tabs.map { it.id }.toSet()
    popupPool.keys.filterNot { it in ids }.toList().forEach { ownerId ->
        popupPool.remove(ownerId).orEmpty().forEach { popup ->
            runCatching { (popup.parent as? ViewGroup)?.removeView(popup) }
            runCatching { popup.stopLoading(); popup.removeAllViews(); popup.destroy() }
        }
    }
    manager.registeredWebViewIds().filterNot { it in ids }.forEach { id ->
        manager.webViewFor(id)?.let { view ->
            runCatching { (view.parent as? ViewGroup)?.removeView(view) }
            runCatching { view.stopLoading(); view.removeAllViews(); view.destroy() }
        }
        manager.forgetWebView(id)
    }
    tabs.forEach { tab ->
        var view = manager.webViewFor(tab.id)
        var created = false
        if (view == null) {
            created = true
            view = WebView(context)
            configureWebView(view, tab, manager, context, host, popupPool, browserPageFailedText)
            manager.registerWebView(tab.id, view)
            host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            view.loadUrl(tab.url)
        } else if (view.parent !== host) {
            (view.parent as? ViewGroup)?.removeView(view)
            host.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        }
        view.visibility = if (tab.id == activeId) View.VISIBLE else View.GONE
        view.bringToFrontIfActive(tab.id == activeId)
        if (!created && view.url != tab.url && tab.loading) view.loadUrl(tab.url)
    }
    popupPool.forEach { (ownerId, popups) ->
        popups.forEach { popup ->
            popup.visibility = if (ownerId == activeId) View.VISIBLE else View.GONE
            if (ownerId == activeId) popup.bringToFront()
        }
    }
}

private fun WebView.bringToFrontIfActive(active: Boolean) {
    if (active) bringToFront()
}

private fun configureWebView(
    webView: WebView,
    tab: AiTabItem,
    manager: TabManagerViewModel,
    context: android.content.Context,
    host: FrameLayout,
    popupPool: MutableMap<String, MutableList<WebView>>,
    browserPageFailedText: String
) {
    val cookies = CookieManager.getInstance()
    cookies.setAcceptCookie(true)
    cookies.setAcceptThirdPartyCookies(webView, true)
    webView.settings.apply {
        javaScriptEnabled = true
        domStorageEnabled = true
        databaseEnabled = true
        loadsImagesAutomatically = true
        javaScriptCanOpenWindowsAutomatically = true
        setSupportMultipleWindows(true)
        safeBrowsingEnabled = true
        // Preserve the platform WebView identity. Spoofing Chrome does not make an embedded WebView
        // a supported OAuth user-agent and can violate provider sign-in restrictions.
        // Keep the platform default user-agent; do not spoof Chrome to bypass provider sign-in restrictions.
    }
    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            val current = view ?: return
            manager.updatePage(tab.id, current.url ?: tab.url, current.title.orEmpty(), newProgress < 100, current.canGoBack(), current.canGoForward())
        }

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            if (!isUserGesture || resultMsg == null) return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val popup = WebView(context)
            cookies.setAcceptCookie(true)
            cookies.setAcceptThirdPartyCookies(popup, true)
            popup.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                safeBrowsingEnabled = true
                // Keep the platform default user-agent; do not spoof Chrome to bypass provider sign-in restrictions.
            }
            popup.webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                    val uri = request.url
                    return routeWebNavigation(context, uri)
                }
            }
            popup.webChromeClient = object : WebChromeClient() {
                override fun onCloseWindow(window: WebView) {
                    runCatching { (window.parent as? ViewGroup)?.removeView(window) }
                    runCatching { window.stopLoading(); window.removeAllViews(); window.destroy() }
                    popupPool[tab.id]?.remove(window)
                    if (manager.activeTabId.value == tab.id) {
                        manager.webViewFor(tab.id)?.visibility = View.VISIBLE
                    }
                }
            }
            host.addView(popup, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            popupPool.getOrPut(tab.id) { mutableListOf() }.add(popup)
            transport.webView = popup
            resultMsg.sendToTarget()
            popup.bringToFront()
            return true
        }
    }
    webView.webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            val uri = request.url
            return routeWebNavigation(context, uri)
        }

        override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
            manager.updatePage(tab.id, url ?: tab.url, view.title.orEmpty(), true, view.canGoBack(), view.canGoForward())
        }

        override fun onPageFinished(view: WebView, url: String?) {
            CookieManager.getInstance().flush()
            manager.updatePage(tab.id, url ?: tab.url, view.title.orEmpty(), false, view.canGoBack(), view.canGoForward())
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            if (request.isForMainFrame) manager.updatePage(tab.id, view.url ?: tab.url, browserPageFailedText, false, view.canGoBack(), view.canGoForward())
        }
    }
}

private fun routeWebNavigation(context: Context, uri: Uri): Boolean {
    if (uri.scheme == "http" || uri.scheme == "https") {
        val host = uri.host?.lowercase().orEmpty()
        val identityHost = host == "accounts.google.com" || host == "login.microsoftonline.com" ||
            host == "login.live.com" || host == "appleid.apple.com" || host == "auth.openai.com" || host.endsWith(".auth0.com")
        if (!identityHost) return false
        // Google OAuth forbids authorization inside an app-controlled embedded user-agent.
        openExternalBrowser(context, uri.toString())
        return true
    }
    return openExternalIntent(context, uri)
}

private fun openExternalIntent(context: android.content.Context, uri: Uri): Boolean {
    return runCatching {
        val intent = if (uri.scheme == "intent") {
            Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
        } else {
            Intent(Intent.ACTION_VIEW, uri)
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrElse { false }
}

private fun openExternalBrowser(context: Context, url: String?) {
    val uri = url?.let { runCatching { Uri.parse(it) }.getOrNull() } ?: return
    runCatching {
        androidx.browser.customtabs.CustomTabsIntent.Builder().build().launchUrl(context, uri)
    }.recoverCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun makePromptInjectionScript(prompt: String, provider: AiProvider): String {
    val escaped = JSONObject.quote(prompt)
    val selectors = when (provider) {
        AiProvider.GEMINI -> "['rich-textarea [contenteditable=true]','.ql-editor[contenteditable=true]','textarea[aria-label*=prompt i]','textarea']"
        AiProvider.CHATGPT -> "['#prompt-textarea','[data-testid=prompt-textarea]','div[contenteditable=true][data-placeholder]','textarea']"
        AiProvider.CLAUDE -> "['div[contenteditable=true]','.ProseMirror','textarea']"
        AiProvider.DEEPSEEK -> "['textarea','div[contenteditable=true]']"
        AiProvider.COPILOT -> "['textarea','div[contenteditable=true]']"
        else -> "['textarea','[contenteditable=true][role=textbox]','div[contenteditable=true]']"
    }
    return """
        (() => {
          const text = $escaped;
          const selectors = $selectors;
          let target = null;
          for (const selector of selectors) {
            try {
              target = document.querySelector(selector);
              if (target && target.getClientRects().length && !target.disabled && !target.readOnly) break;
              target = null;
            } catch (_) { target = null; }
          }
          if (!target) return 'no-input';
          target.focus();
          if (target.isContentEditable) {
            target.textContent = text;
            target.dispatchEvent(new InputEvent('input', {bubbles:true, inputType:'insertText', data:text}));
          } else {
            const proto = target instanceof HTMLTextAreaElement ? HTMLTextAreaElement.prototype : HTMLInputElement.prototype;
            const setter = Object.getOwnPropertyDescriptor(proto, 'value')?.set;
            if (setter) setter.call(target, text); else target.value = text;
            target.dispatchEvent(new InputEvent('input', {bubbles:true, inputType:'insertText', data:text}));
            target.dispatchEvent(new Event('change', {bubbles:true}));
          }
          const buttons = [...document.querySelectorAll('button')].filter(b => !b.disabled && b.getClientRects().length);
          const send = buttons.find(b => /send|submit|send message/i.test((b.getAttribute('aria-label') || '') + ' ' + (b.getAttribute('title') || '') + ' ' + (b.innerText || ''))) ||
            document.querySelector('button[data-testid*=send],button[type=submit]');
          if (send && !send.disabled) {
            send.click();
            return 'sent';
          }
          const form = target.closest('form');
          if (form && form.requestSubmit) {
            form.requestSubmit();
            return 'sent';
          }
          return 'no-send';
        })();
    """.trimIndent()
}
