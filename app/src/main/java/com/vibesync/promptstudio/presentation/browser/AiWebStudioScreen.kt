package com.vibesync.promptstudio.presentation.browser

import android.app.DownloadManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.URLUtil
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewDatabase
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tab
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.webkit.ProfileStore
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.vibesync.promptstudio.R
import org.json.JSONArray
import org.json.JSONTokener
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiWebStudioScreen(
    onOpenDeveloperApi: () -> Unit,
    onOpenDeveloperResources: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val density = LocalDensity.current
    val manager: TabManagerViewModel = viewModel()
    val tabs by manager.tabs.collectAsState()
    val activeId by manager.activeTabId.collectAsState()
    val activeTab = tabs.firstOrNull { it.id == activeId }
    val keyboardVisible = androidx.compose.foundation.layout.WindowInsets.ime.getBottom(density) > 0

    var showProviderSheet by remember { mutableStateOf(false) }
    var showTabOverview by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }
    var showBrowserSettingsSheet by remember { mutableStateOf(false) }
    var showClearBrowsingDataDialog by remember { mutableStateOf(false) }
    var showFindBar by remember { mutableStateOf(false) }
    var findQuery by remember { mutableStateOf("") }
    var findCount by remember { mutableStateOf(0) }
    var findOrdinal by remember { mutableStateOf(0) }
    var showCertificateDialog by remember { mutableStateOf(false) }
    var certificateSubject by remember { mutableStateOf("") }
    var certificateIssuer by remember { mutableStateOf("") }
    var certificateCipher by remember { mutableStateOf("") }
    var browserMenuExpanded by remember { mutableStateOf(false) }
    val browserPreferences = remember(context) { context.getSharedPreferences("vibesync_browser_preferences", 0) }
    var desktopMode by remember(browserPreferences) { mutableStateOf(browserPreferences.getBoolean("desktop_mode", false)) }
    var textZoom by remember(browserPreferences) { mutableStateOf(browserPreferences.getInt("text_zoom", 100).coerceIn(50, 200)) }
    var darkWebContent by remember(browserPreferences) { mutableStateOf(browserPreferences.getBoolean("dark_web_content", false)) }
    var acceptThirdPartyCookies by remember(browserPreferences) { mutableStateOf(browserPreferences.getBoolean("third_party_cookies", true)) }
    var savedBookmarks by remember(context) { mutableStateOf(loadBrowserBookmarks(context)) }
    val pendingBrowserPrompt by BrowserPromptBridge.pending.collectAsState()
    var addressBarExpanded by remember { mutableStateOf(true) }
    var address by remember(activeId, activeTab?.url) { mutableStateOf(activeTab?.url.orEmpty()) }
    var addressSuggestionsVisible by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var hostContainer by remember { mutableStateOf<FrameLayout?>(null) }
    val popupPool = remember { mutableMapOf<String, MutableList<WebView>>() }

    val browserPageFailedText = stringResource(R.string.browser_page_failed)
    val promptSentText = stringResource(R.string.prompt_sent)
    val promptFilledText = stringResource(R.string.prompt_filled_no_send)
    val promptNoInputText = stringResource(R.string.prompt_input_not_found)
    val promptUnsupportedText = stringResource(R.string.prompt_injection_unsupported)
    val bookmarkSavedText = stringResource(R.string.browser_bookmark_saved)
    val pageCopiedText = stringResource(R.string.browser_super_copy_success)
    val pageEmptyText = stringResource(R.string.browser_super_copy_empty)
    val privateModeUnavailableText = stringResource(R.string.browser_settings_private_mode_unavailable)
    val browsingDataClearedText = stringResource(R.string.browser_settings_data_cleared)
    val certificateUnavailableText = stringResource(R.string.browser_settings_certificate_unavailable)
    val secureSiteRequiredText = stringResource(R.string.browser_settings_secure_site_required)
    val tlsUnavailableText = stringResource(R.string.browser_settings_tls_unavailable)
    val currentHost = activeTab?.url?.let { url ->
        runCatching { Uri.parse(url).host.orEmpty() }.getOrNull()
    }.orEmpty()
    val isSecureConnection = activeTab?.url?.startsWith("https://", ignoreCase = true) == true

    LaunchedEffect(pendingBrowserPrompt?.id, activeId, activeTab?.url) {
        val request = pendingBrowserPrompt ?: return@LaunchedEffect
        val tab = activeTab
        val webView = tab?.let { manager.webViewFor(it.id) }
        if (tab == null || webView == null) {
            feedback = promptNoInputText
        } else if (!tab.provider.supportsPromptInjection(tab.url)) {
            feedback = promptUnsupportedText
        } else {
            webView.evaluateJavascript(makePromptInjectionScript(request.text, tab.provider)) { result ->
                feedback = when {
                    result?.contains("no-input") == true -> promptNoInputText
                    result?.contains("no-send") == true -> promptFilledText
                    else -> promptSentText
                }
            }
        }
        BrowserPromptBridge.consume(request.id)
    }

    LaunchedEffect(feedback) {
        feedback?.let { message -> Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
    }

    LaunchedEffect(Unit) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
            val liveProfiles = manager.tabs.value.filter { it.isPrivate }.mapNotNull { it.profileName }.toSet()
            runCatching {
                ProfileStore.getInstance().getAllProfileNames()
                    .filter { it.startsWith("vibesync_private_") && it !in liveProfiles }
                    .forEach { orphan -> ProfileStore.getInstance().deleteProfile(orphan) }
            }
        }
    }

    LaunchedEffect(showCertificateDialog, activeId, activeTab?.url) {
        if (!showCertificateDialog) return@LaunchedEffect
        val webView = activeTab?.let { manager.webViewFor(it.id) }
        val cert = webView?.certificate
        certificateSubject = cert?.issuedTo?.dName?.takeIf { it.isNotBlank() } ?: certificateUnavailableText
        certificateIssuer = cert?.issuedBy?.dName?.takeIf { it.isNotBlank() } ?: certificateUnavailableText
        val uri = activeTab?.url?.let { runCatching { Uri.parse(it) }.getOrNull() }
        val host = uri?.host
        certificateCipher = if (uri?.scheme.equals("https", ignoreCase = true) && !host.isNullOrBlank()) {
            withContext(Dispatchers.IO) {
                runCatching {
                    Socket().use { rawSocket ->
                        rawSocket.connect(InetSocketAddress(host, if (uri.port > 0) uri.port else 443), 4000)
                        val sslSocket = (SSLSocketFactory.getDefault() as SSLSocketFactory)
                            .createSocket(rawSocket, host, if (uri.port > 0) uri.port else 443, true) as SSLSocket
                        sslSocket.use { secureSocket ->
                            secureSocket.soTimeout = 4000
                            val parameters = secureSocket.sslParameters
                            parameters.endpointIdentificationAlgorithm = "HTTPS"
                            secureSocket.sslParameters = parameters
                            secureSocket.startHandshake()
                            "${secureSocket.session.protocol} · ${secureSocket.session.cipherSuite}"
                        }
                    }
                }.getOrDefault(tlsUnavailableText)
            }
        } else {
            secureSiteRequiredText
        }
    }

    LaunchedEffect(showFindBar, activeId, activeTab?.url, findQuery) {
        val webView = activeTab?.let { manager.webViewFor(it.id) }
        if (!showFindBar || webView == null) {
            findCount = 0
            findOrdinal = 0
            return@LaunchedEffect
        }
        webView.setFindListener { activeMatchOrdinal, numberOfMatches, _ ->
            findCount = numberOfMatches
            findOrdinal = if (numberOfMatches > 0) activeMatchOrdinal + 1 else 0
        }
        if (findQuery.isBlank()) {
            webView.clearMatches()
            findCount = 0
            findOrdinal = 0
        } else {
            webView.findAllAsync(findQuery)
        }
    }

    BackHandler(enabled = activeTab != null) {
        val activeView = activeTab?.let { manager.webViewFor(it.id) }
        if (activeView?.canGoBack() == true) activeView.goBack() else onBack()
    }

    /*
     * The root owns IME and navigation-bar insets. This avoids applying imePadding twice:
     * the WebView's weighted container is measured inside the remaining height, so its
     * native viewport shrinks for both Compose prompt focus and web-form focus.
     */
    Column(
        modifier = Modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        BrowserFavicon(tab = activeTab, modifier = Modifier.size(36.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            val title = activeTab?.title?.takeIf { it.isNotBlank() }
                                ?: activeTab?.let { stringResource(it.provider.titleRes) }
                                ?: stringResource(R.string.ai_web_studio)
                            Text(
                                text = title,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Row(
                                modifier = Modifier.clickable(enabled = activeTab != null) {
                                    showCertificateDialog = true
                                },
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSecureConnection) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = stringResource(
                                        if (isSecureConnection) R.string.browser_secure_connection
                                        else R.string.browser_insecure_connection
                                    ),
                                    tint = if (isSecureConnection) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = if (activeTab == null) stringResource(R.string.browser_mode_label)
                                        else if (activeTab.isPrivate) stringResource(R.string.browser_settings_private_host, currentHost.ifBlank { activeTab.url })
                                        else currentHost.ifBlank { activeTab.url },
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { addressBarExpanded = !addressBarExpanded }) {
                        Icon(
                            imageVector = if (addressBarExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = stringResource(
                                if (addressBarExpanded) R.string.browser_address_hide else R.string.browser_address_show
                            )
                        )
                    }
                    IconButton(
                        onClick = {
                            val tab = activeTab
                            val webView = tab?.let { manager.webViewFor(it.id) }
                            if (tab != null && webView != null && tab.loading) {
                                webView.stopLoading()
                                manager.updatePage(tab.id, webView.url ?: tab.url, webView.title.orEmpty(), false, webView.canGoBack(), webView.canGoForward())
                            } else {
                                manager.reload()
                            }
                        },
                        enabled = activeTab != null
                    ) {
                        Icon(
                            imageVector = if (activeTab?.loading == true) Icons.Default.Stop else Icons.Default.Refresh,
                            contentDescription = stringResource(
                                if (activeTab?.loading == true) R.string.browser_stop_loading else R.string.browser_reload
                            )
                        )
                    }
                    IconButton(onClick = { showTabOverview = true }) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(Icons.Default.Tab, contentDescription = null)
                            Text(tabs.size.toString(), style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Box {
                        IconButton(onClick = { browserMenuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.mode_api))
                        }
                        DropdownMenu(
                            expanded = browserMenuExpanded,
                            onDismissRequest = { browserMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_settings_title)) },
                                onClick = { browserMenuExpanded = false; showBrowserSettingsSheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_settings_find_in_page)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    showFindBar = true
                                    findQuery = ""
                                    findCount = 0
                                    findOrdinal = 0
                                },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_settings_print_pdf)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    printBrowserPage(context, manager.webViewFor(activeId), activeTab?.title.orEmpty())
                                },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_settings_new_private_tab)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    if (!manager.openPrivateTab(AiProvider.GEMINI)) feedback = privateModeUnavailableText
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_bookmark_current_page)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    activeTab?.let { tab ->
                                        savedBookmarks = addBrowserBookmark(context, tab.title.ifBlank { Uri.parse(tab.url).host.orEmpty() }, tab.url)
                                        feedback = bookmarkSavedText
                                    }
                                },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_open_bookmarks)) },
                                onClick = { browserMenuExpanded = false; showBookmarksSheet = true }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.developer_resources)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    onOpenDeveloperResources()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_super_copy_text)) },
                                onClick = { browserMenuExpanded = false; extractPageToClipboard(context, manager.webViewFor(activeId), markdown = false, onResult = { text -> feedback = if (text) pageCopiedText else pageEmptyText }) },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.browser_super_copy_markdown)) },
                                onClick = { browserMenuExpanded = false; extractPageToClipboard(context, manager.webViewFor(activeId), markdown = true, onResult = { text -> feedback = if (text) pageCopiedText else pageEmptyText }) },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(if (desktopMode) R.string.browser_mobile_mode else R.string.browser_desktop_mode)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    desktopMode = !desktopMode
                                    browserPreferences.edit().putBoolean("desktop_mode", desktopMode).apply()
                                    tabs.forEach { tab -> manager.webViewFor(tab.id)?.let { applyDesktopLayout(it, context, desktopMode) } }
                                    manager.webViewFor(activeId)?.reload()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.open_external_browser)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    openExternalBrowser(context, activeTab?.url)
                                },
                                enabled = activeTab != null
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.mode_api)) },
                                onClick = {
                                    browserMenuExpanded = false
                                    onOpenDeveloperApi()
                                }
                            )
                        }
                    }
                }
            )

            AnimatedVisibility(visible = addressBarExpanded && !keyboardVisible) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val providerSites = listOf(
                        BrowserBookmark(stringResource(R.string.provider_gemini_short), AiProvider.GEMINI.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_chatgpt_short), AiProvider.CHATGPT.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_claude_short), AiProvider.CLAUDE.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_copilot_short), AiProvider.COPILOT.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_deepseek_short), AiProvider.DEEPSEEK.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_mistral_short), AiProvider.MISTRAL.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_meta_ai_short), AiProvider.META_AI.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_perplexity_short), AiProvider.PERPLEXITY.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_huggingchat_short), AiProvider.HUGGINGCHAT.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_duckduckgo_short), AiProvider.DUCKDUCKGO.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_poe_short), AiProvider.POE.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_qwen_short), AiProvider.QWEN.startUrl),
                        BrowserBookmark(stringResource(R.string.provider_phind_short), AiProvider.PHIND.startUrl)
                    )
                    val suggestions = remember(address, savedBookmarks, providerSites) {
                        (savedBookmarks + providerSites)
                            .distinctBy { it.url }
                            .filter { address.isNotBlank() && (it.url.contains(address, ignoreCase = true) || it.title.contains(address, ignoreCase = true)) }
                            .take(6)
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it; addressSuggestionsVisible = it.isNotBlank() },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            label = { Text(stringResource(R.string.address_bar)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                        )
                        DropdownMenu(
                            expanded = addressSuggestionsVisible && suggestions.isNotEmpty(),
                            onDismissRequest = { addressSuggestionsVisible = false }
                        ) {
                            suggestions.forEach { suggestion ->
                                DropdownMenuItem(
                                    text = {
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(suggestion.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(suggestion.url, style = MaterialTheme.typography.labelSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    },
                                    onClick = { address = suggestion.url; addressSuggestionsVisible = false }
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            if (address.isNotBlank()) {
                                if (activeTab == null) manager.openTab(AiProvider.CUSTOM, address)
                                else manager.navigate(activeTab.id, address)
                                addressSuggestionsVisible = false
                                feedback = null
                                focusManager.clearFocus()
                            }
                        },
                        enabled = address.isNotBlank()
                    ) {
                        Text(stringResource(R.string.go))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 1.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                IconButton(
                    onClick = {
                        val webView = activeTab?.let { manager.webViewFor(it.id) }
                        if (webView?.canGoBack() == true) webView.goBack() else onBack()
                    },
                    enabled = activeTab != null
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.browser_back))
                }
                IconButton(
                    onClick = { manager.goForward() },
                    enabled = activeTab?.canGoForward == true
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = stringResource(R.string.browser_forward))
                }
                IconButton(
                    onClick = {
                        if (activeTab == null) manager.openTab(AiProvider.GEMINI)
                        else manager.navigate(activeTab.id, activeTab.provider.startUrl)
                        focusManager.clearFocus()
                    }
                ) {
                    Icon(Icons.Default.Home, contentDescription = stringResource(R.string.browser_home))
                }
                IconButton(onClick = { showProviderSheet = true }) {
                    Icon(Icons.Default.Add, contentDescription = stringResource(R.string.open_provider_directory))
                }
                IconButton(onClick = { showTabOverview = true }) {
                    Icon(Icons.Default.Tab, contentDescription = stringResource(R.string.tab_overview))
                }
            }
        }

        if (showFindBar) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                OutlinedTextField(
                    value = findQuery,
                    onValueChange = { findQuery = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    label = { Text(stringResource(R.string.browser_settings_find_hint)) }
                )
                Text(
                    text = stringResource(R.string.browser_settings_find_count, findOrdinal, findCount),
                    style = MaterialTheme.typography.labelMedium
                )
                TextButton(onClick = { manager.webViewFor(activeId)?.findNext(false) }, enabled = findCount > 0) {
                    Text(stringResource(R.string.browser_settings_previous_match))
                }
                TextButton(onClick = { manager.webViewFor(activeId)?.findNext(true) }, enabled = findCount > 0) {
                    Text(stringResource(R.string.browser_settings_next_match))
                }
                TextButton(onClick = {
                    manager.webViewFor(activeId)?.clearMatches()
                    findQuery = ""
                    showFindBar = false
                }) {
                    Text(stringResource(R.string.browser_settings_close_find))
                }
            }
        }

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
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
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.TopCenter).padding(10.dp).size(24.dp)
                )
            }
            if (activeTab == null) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(stringResource(R.string.no_open_tabs), style = MaterialTheme.typography.titleMedium)
                    Button(onClick = { showProviderSheet = true }) {
                        Text(stringResource(R.string.open_provider_directory))
                    }
                }
            }
        }

    }

    if (showProviderSheet) {
        ModalBottomSheet(onDismissRequest = { showProviderSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    stringResource(R.string.provider_directory),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    stringResource(R.string.provider_directory_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().heightIn(max = 440.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(AiProvider.entries.filterNot { it == AiProvider.CUSTOM }) { provider ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(provider.icon, fontWeight = FontWeight.Bold)
                                    }
                                }
                                Text(
                                    stringResource(provider.titleRes),
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                FilledTonalButton(
                                    onClick = {
                                        manager.openTab(provider)
                                        showProviderSheet = false
                                        feedback = null
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(stringResource(R.string.open_tab))
                                }
                            }
                        }
                    }
                }
                var customUrl by remember { mutableStateOf("") }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customUrl,
                        onValueChange = { customUrl = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        label = { Text(stringResource(R.string.custom_address)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
                    )
                    Button(
                        onClick = {
                            if (customUrl.isNotBlank()) {
                                manager.openTab(AiProvider.CUSTOM, customUrl)
                                showProviderSheet = false
                            }
                        },
                        enabled = customUrl.isNotBlank()
                    ) {
                        Text(stringResource(R.string.open_tab))
                    }
                }
            }
        }
    }

    if (showBookmarksSheet) {
        ModalBottomSheet(onDismissRequest = { showBookmarksSheet = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.browser_bookmarks_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (savedBookmarks.isEmpty()) {
                    Text(stringResource(R.string.browser_no_bookmarks_saved), color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth().heightIn(max = 520.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(savedBookmarks, key = { it.url }) { bookmark ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).clickable {
                                            manager.openTab(AiProvider.fromHost(bookmark.url) ?: AiProvider.CUSTOM, bookmark.url)
                                            showBookmarksSheet = false
                                        }.padding(vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Text(bookmark.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(bookmark.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    IconButton(onClick = { savedBookmarks = removeBrowserBookmark(context, bookmark.url) }) {
                                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.browser_bookmark_remove))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTabOverview) {
        ModalBottomSheet(onDismissRequest = { showTabOverview = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.open_tabs_title, tabs.size),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { showTabOverview = false }) {
                        Text(stringResource(R.string.done))
                    }
                }
                Text(
                    stringResource(R.string.browser_tabs_reorder_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (tabs.isEmpty()) {
                    Text(stringResource(R.string.no_open_tabs))
                } else {
                    val itemDensity = LocalDensity.current
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 156.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(max = 560.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(tabs, key = { it.id }) { tab ->
                            val tabIndex = tabs.indexOfFirst { it.id == tab.id }
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .pointerInput(tab.id, tabs.size) {
                                        var totalDrag = 0f
                                        detectDragGesturesAfterLongPress(
                                            onDragEnd = {
                                                val slotSize = with(itemDensity) { 170.dp.toPx() }
                                                val delta = (totalDrag / slotSize).toInt()
                                                if (delta != 0) {
                                                    manager.reorderTabs(
                                                        tabIndex,
                                                        (tabIndex + delta).coerceIn(0, tabs.lastIndex)
                                                    )
                                                }
                                            },
                                            onDragCancel = { totalDrag = 0f },
                                            onDrag = { change, amount ->
                                                change.consume()
                                                totalDrag += amount.x
                                            }
                                        )
                                    }
                                    .pointerInput(tab.id) {
                                        var drag = 0f
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { change, amount ->
                                                change.consume()
                                                drag += amount
                                            },
                                            onDragEnd = {
                                                if (kotlin.math.abs(drag) > 88.dp.toPx()) manager.closeTab(tab.id)
                                            }
                                        )
                                    }
                                    .clickable {
                                        manager.switchTab(tab.id)
                                        showTabOverview = false
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (tab.id == activeId)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(88.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(3.dp)
                                        ) {
                                            BrowserFavicon(tab = tab, modifier = Modifier.size(32.dp))
                                            Text(
                                                text = tab.title.ifBlank { stringResource(tab.provider.shortTitleRes) },
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelMedium
                                            )
                                            Text(
                                                text = Uri.parse(tab.url).host.orEmpty(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stringResource(tab.provider.shortTitleRes),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = if (tab.isPrivate) stringResource(R.string.browser_settings_private_tab)
                                                    else if (tab.loading) stringResource(R.string.model_discovery_loading) else Uri.parse(tab.url).host.orEmpty(),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                        }
                                        IconButton(onClick = { manager.closeTab(tab.id) }) {
                                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close_tab))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBrowserSettingsSheet) {
        BrowserSettingsSheet(
            desktopMode = desktopMode,
            textZoom = textZoom,
            darkContent = darkWebContent,
            thirdPartyCookies = acceptThirdPartyCookies,
            onDesktopModeChange = { enabled ->
                desktopMode = enabled
                browserPreferences.edit().putBoolean("desktop_mode", enabled).apply()
                tabs.forEach { tab -> manager.webViewFor(tab.id)?.let { applyDesktopLayout(it, context, enabled) } }
                popupPool.values.flatten().forEach { applyDesktopLayout(it, context, enabled) }
                manager.webViewFor(activeId)?.reload()
            },
            onTextZoomChange = { zoom ->
                textZoom = zoom
                browserPreferences.edit().putInt("text_zoom", zoom).apply()
                tabs.forEach { tab -> manager.webViewFor(tab.id)?.settings?.textZoom = zoom }
                popupPool.values.flatten().forEach { it.settings.textZoom = zoom }
            },
            onDarkContentChange = { enabled ->
                darkWebContent = enabled
                browserPreferences.edit().putBoolean("dark_web_content", enabled).apply()
                tabs.forEach { tab -> manager.webViewFor(tab.id)?.let { setWebContentDarkening(it, enabled) } }
                popupPool.values.flatten().forEach { setWebContentDarkening(it, enabled) }
            },
            onThirdPartyCookiesChange = { enabled ->
                acceptThirdPartyCookies = enabled
                browserPreferences.edit().putBoolean("third_party_cookies", enabled).apply()
                tabs.forEach { tab -> manager.webViewFor(tab.id)?.let { setThirdPartyCookiePolicy(it, enabled) } }
                popupPool.values.flatten().forEach { setThirdPartyCookiePolicy(it, enabled) }
            },
            onClearBrowsingData = {
                showBrowserSettingsSheet = false
                showClearBrowsingDataDialog = true
            },
            onDismiss = { showBrowserSettingsSheet = false }
        )
    }

    if (showClearBrowsingDataDialog) {
        ClearBrowsingDataDialog(
            onDismiss = { showClearBrowsingDataDialog = false },
            onConfirm = { cache, cookies, storage, forms ->
                clearBrowserData(context, manager, tabs, popupPool, cache, cookies, storage, forms)
                showClearBrowsingDataDialog = false
                feedback = browsingDataClearedText
            }
        )
    }

    if (showCertificateDialog) {
        AlertDialog(
            onDismissRequest = { showCertificateDialog = false },
            title = { Text(stringResource(R.string.browser_settings_certificate_title)) },
            text = {
                androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.browser_settings_certificate_subject), fontWeight = FontWeight.Bold)
                    Text(certificateSubject.ifBlank { certificateUnavailableText })
                    Text(stringResource(R.string.browser_settings_certificate_issuer), fontWeight = FontWeight.Bold)
                    Text(certificateIssuer.ifBlank { certificateUnavailableText })
                    Text(stringResource(R.string.browser_settings_certificate_cipher), fontWeight = FontWeight.Bold)
                    Text(certificateCipher.ifBlank { certificateUnavailableText })
                }
            },
            confirmButton = {
                TextButton(onClick = { showCertificateDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            }
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            // Main tab WebViews remain alive when this destination stays on the back stack.
            hostContainer?.let { host ->
                for (i in 0 until host.childCount) host.getChildAt(i).visibility = View.GONE
            }
            activeTab?.let { manager.webViewFor(it.id)?.clearMatches() }
            popupPool.values.flatten().forEach { popup ->
                runCatching { (popup.parent as? ViewGroup)?.removeView(popup) }
                runCatching { popup.stopLoading(); popup.removeAllViews(); popup.destroy() }
            }
            popupPool.clear()
        }
    }
}

@Composable
private fun BrowserFavicon(tab: AiTabItem?, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center) {
            val icon = tab?.favicon
            if (icon != null) {
                androidx.compose.foundation.Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = stringResource(R.string.browser_tab_preview),
                    modifier = Modifier.fillMaxSize().padding(5.dp)
                )
            } else {
                Text(
                    text = tab?.provider?.icon ?: "✦",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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
    val density = LocalDensity.current
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

private data class BrowserBookmark(val title: String, val url: String)

private fun loadBrowserBookmarks(context: Context): List<BrowserBookmark> {
    val raw = context.getSharedPreferences("vibesync_browser_preferences", 0).getString("bookmarks", "[]")
    return runCatching {
        val array = JSONArray(raw)
        (0 until array.length()).mapNotNull { index ->
            val item = array.optJSONObject(index) ?: return@mapNotNull null
            val title = item.optString("title").take(180)
            val url = item.optString("url")
            if (title.isBlank() || !url.startsWith("https://") && !url.startsWith("http://")) null
            else BrowserBookmark(title, url)
        }
    }.getOrDefault(emptyList())
}

private fun persistBrowserBookmarks(context: Context, bookmarks: List<BrowserBookmark>) {
    val array = JSONArray()
    bookmarks.forEach { item ->
        array.put(JSONObject().put("title", item.title).put("url", item.url))
    }
    context.getSharedPreferences("vibesync_browser_preferences", 0)
        .edit().putString("bookmarks", array.toString()).apply()
}

private fun addBrowserBookmark(context: Context, title: String, url: String): List<BrowserBookmark> {
    val next = (loadBrowserBookmarks(context).filterNot { it.url == url } + BrowserBookmark(title.take(180), url)).takeLast(100)
    persistBrowserBookmarks(context, next)
    return next
}

private fun removeBrowserBookmark(context: Context, url: String): List<BrowserBookmark> {
    val next = loadBrowserBookmarks(context).filterNot { it.url == url }
    persistBrowserBookmarks(context, next)
    return next
}

private fun applyDesktopLayout(webView: WebView, context: Context, enabled: Boolean) {
    val mobileUserAgent = WebSettings.getDefaultUserAgent(context)
    val desktopUserAgent = mobileUserAgent
        .replace(Regex("\\(Linux; Android [^)]+\\)"), "(X11; Linux x86_64)")
        .replace(" Version/4.0", "")
        .replace(" Mobile", "")
        .replace("; wv", "")
    webView.settings.apply {
        useWideViewPort = true
        loadWithOverviewMode = enabled
        userAgentString = if (enabled) desktopUserAgent else mobileUserAgent
    }
}

private fun cookieManagerFor(webView: WebView): CookieManager {
    return if (WebViewFeature.isFeatureSupported(WebViewFeature.MULTI_PROFILE)) {
        runCatching { WebViewCompat.getProfile(webView).cookieManager }.getOrElse { CookieManager.getInstance() }
    } else {
        CookieManager.getInstance()
    }
}

private fun setWebContentDarkening(webView: WebView, enabled: Boolean) {
    val settings = webView.settings
    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        runCatching {
            WebSettingsCompat.setForceDark(
                settings,
                if (enabled) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }
    if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
        runCatching { WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, enabled) }
    }
}

private fun setThirdPartyCookiePolicy(webView: WebView, enabled: Boolean) {
    runCatching { cookieManagerFor(webView).setAcceptThirdPartyCookies(webView, enabled) }
}

private fun applyBrowserPreferences(webView: WebView, context: Context) {
    val preferences = context.getSharedPreferences("vibesync_browser_preferences", Context.MODE_PRIVATE)
    applyDesktopLayout(webView, context, preferences.getBoolean("desktop_mode", false))
    webView.settings.textZoom = preferences.getInt("text_zoom", 100).coerceIn(50, 200)
    setWebContentDarkening(webView, preferences.getBoolean("dark_web_content", false))
    setThirdPartyCookiePolicy(webView, preferences.getBoolean("third_party_cookies", true))
}

private fun clearBrowserData(
    context: Context,
    manager: TabManagerViewModel,
    tabs: List<AiTabItem>,
    popupPool: Map<String, List<WebView>>,
    clearCache: Boolean,
    clearCookies: Boolean,
    clearStorage: Boolean,
    clearForms: Boolean
) {
    val views = (manager.registeredWebViewIds().mapNotNull { manager.webViewFor(it) } +
        popupPool.values.flatten()).distinct()
    if (clearCache) views.forEach { runCatching { it.clearCache(true) } }
    if (clearCookies) {
        runCatching { CookieManager.getInstance().removeAllCookies(null) }
        views.filter { view -> tabs.any { it.isPrivate && manager.webViewFor(it.id) === view } }
            .forEach { view -> runCatching { cookieManagerFor(view).removeAllCookies(null) } }
        runCatching { CookieManager.getInstance().flush() }
    }
    if (clearStorage) {
        runCatching { WebStorage.getInstance().deleteAllData() }
        views.filter { view -> tabs.any { it.isPrivate && manager.webViewFor(it.id) === view } }
            .forEach { view ->
                runCatching { WebViewCompat.getProfile(view).webStorage.deleteAllData() }
            }
    }
    if (clearForms) {
        views.forEach { runCatching { it.clearFormData() } }
        runCatching { WebViewDatabase.getInstance(context).clearFormData() }
    }
}

private fun printBrowserPage(context: Context, webView: WebView?, title: String) {
    if (webView == null) return
    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager ?: return
    val printTitle = title.ifBlank { Uri.parse(webView.url.orEmpty()).host.orEmpty().ifBlank { "VibeSync page" } }
    runCatching {
        printManager.print(
            printTitle,
            webView.createPrintDocumentAdapter(printTitle),
            PrintAttributes.Builder().build()
        )
    }
}

private fun enqueueBrowserDownload(
    context: Context,
    webView: WebView,
    url: String,
    userAgent: String?,
    contentDisposition: String?,
    mimeType: String?
) {
    val message = runCatching {
        val uri = Uri.parse(url)
        require(uri.scheme.equals("https", true) || uri.scheme.equals("http", true))
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType).take(180)
        val request = DownloadManager.Request(uri)
            .setTitle(fileName)
            .setDescription(uri.host.orEmpty())
            .setMimeType(mimeType)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
        userAgent?.takeIf { it.isNotBlank() }?.let { request.addRequestHeader("User-Agent", it) }
        cookieManagerFor(webView).getCookie(url)?.takeIf { it.isNotBlank() }
            ?.let { request.addRequestHeader("Cookie", it) }
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        downloadManager.enqueue(request)
        R.string.browser_settings_download_started
    }.getOrElse { R.string.browser_settings_download_failed }
    Toast.makeText(context, context.getString(message), Toast.LENGTH_LONG).show()
}

private fun extractPageToClipboard(
    context: Context,
    webView: WebView?,
    markdown: Boolean,
    onResult: (Boolean) -> Unit
) {
    if (webView == null) {
        onResult(false)
        return
    }
    val script = if (markdown) {
        """
        (() => {
          const content = document.querySelector('main') || document.querySelector('article') || document.body;
          if (!content) return '';
          const nodes = [...content.querySelectorAll('h1,h2,h3,h4,p,li,pre,blockquote')];
          const pieces = nodes.map(el => {
            const value = (el.innerText || el.textContent || '').trim();
            if (!value) return '';
            if (el.tagName === 'PRE') return '```\n' + value + '\n```';
            if (/^H[1-4]$/.test(el.tagName)) return '#'.repeat(Number(el.tagName.slice(1))) + ' ' + value;
            if (el.tagName === 'LI') return '- ' + value;
            if (el.tagName === 'BLOCKQUOTE') return '> ' + value;
            return value;
          });
          return pieces.filter(Boolean).join('\n\n');
        })()
        """.trimIndent()
    } else {
        """(() => (document.querySelector('main') || document.body)?.innerText || '')()"""
    }
    webView.evaluateJavascript(script) { raw ->
        val extracted = runCatching { JSONTokener(raw ?: "\"\"").nextValue()?.toString().orEmpty() }.getOrDefault("")
        if (extracted.isBlank()) {
            onResult(false)
        } else {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText(if (markdown) "Web page Markdown" else "Web page text", extracted))
            onResult(clipboard != null)
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
    if (tab.isPrivate) {
        WebViewCompat.setProfile(webView, requireNotNull(tab.profileName))
    }
    val cookies = cookieManagerFor(webView)
    cookies.setAcceptCookie(true)
    cookies.setAcceptThirdPartyCookies(webView, context.getSharedPreferences("vibesync_browser_preferences", Context.MODE_PRIVATE).getBoolean("third_party_cookies", true))
    webView.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType ->
        enqueueBrowserDownload(context, webView, url, userAgent, contentDisposition, mimeType)
    })
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
        // Login identity is not spoofed; known identity-provider flows are routed to Custom Tabs.
    }
    applyBrowserPreferences(webView, context)
    webView.webChromeClient = object : WebChromeClient() {
        override fun onReceivedIcon(view: WebView?, icon: Bitmap?) {
            manager.updateFavicon(tab.id, icon)
        }

        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            val current = view ?: return
            manager.updatePage(tab.id, current.url ?: tab.url, current.title.orEmpty(), newProgress < 100, current.canGoBack(), current.canGoForward())
        }

        override fun onCreateWindow(view: WebView?, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message?): Boolean {
            if (tab.isPrivate || !isUserGesture || resultMsg == null) return false
            val transport = resultMsg.obj as? WebView.WebViewTransport ?: return false
            val popup = WebView(context)
            cookies.setAcceptCookie(true)
            popup.setDownloadListener(DownloadListener { url, userAgent, contentDisposition, mimeType ->
                enqueueBrowserDownload(context, popup, url, userAgent, contentDisposition, mimeType)
            })
            cookies.setAcceptThirdPartyCookies(popup, true)
            popup.settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = true
                setSupportMultipleWindows(true)
                safeBrowsingEnabled = true
                // Keep platform sign-in restrictions; identity-provider routes leave the embedded view.
            }
            applyBrowserPreferences(popup, context)
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
