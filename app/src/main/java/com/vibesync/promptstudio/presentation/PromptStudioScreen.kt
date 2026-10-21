package com.vibesync.promptstudio.presentation

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.ads.AdMobManager
import com.vibesync.promptstudio.presentation.common.ModelSelector
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptStudioScreen(
    viewModel: PromptStudioViewModel,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenDeveloperResources: () -> Unit,
    onOpenWebStudio: () -> Unit,
    connectivityStatus: ConnectivityStatus,
    isOnline: Boolean,
    adMobManager: AdMobManager,
    onSuccessfulPromptRun: (String) -> Unit,
    showTopBar: Boolean = true
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var lastHandledSuccess by remember { mutableIntStateOf(state.successfulRunCount) }
    LaunchedEffect(state.successfulRunCount) {
        if (state.successfulRunCount > lastHandledSuccess) onSuccessfulPromptRun(state.provider.displayName)
        lastHandledSuccess = state.successfulRunCount
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val exportChooserTitle = stringResource(R.string.export_response_chooser)
    var topBarMenuExpanded by remember { mutableStateOf(false) }
    LaunchedEffect(state.provider) { viewModel.refreshModels() }
    val bannerLoadFailed by adMobManager.bannerLoadFailed.collectAsStateWithLifecycle()
    val bannerLoaded by adMobManager.bannerLoaded.collectAsStateWithLifecycle()
    val canRequestAds by adMobManager.canRequestAds.collectAsStateWithLifecycle()
    val adsReady by adMobManager.adsReady.collectAsStateWithLifecycle()
    LaunchedEffect(isOnline) { if (isOnline) adMobManager.resetBannerFailure() }
    Scaffold(
        modifier = Modifier.fillMaxSize().imePadding(),
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
        if (showTopBar) TopAppBar(
            windowInsets = WindowInsets.statusBars,
            title = {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Image(painter = painterResource(R.drawable.ic_launcher), contentDescription = null, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.app_brand_title),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        Text(
                            stringResource(R.string.app_brand_subtitle),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            actions = {
                IconButton(onClick = onOpenWebStudio) { Icon(Icons.Default.Web, contentDescription = stringResource(R.string.ai_web_studio)) }
                Box {
                    IconButton(onClick = { topBarMenuExpanded = true }) { Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.settings)) }
                    DropdownMenu(expanded = topBarMenuExpanded, onDismissRequest = { topBarMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text(stringResource(R.string.diagnostics)) }, onClick = { topBarMenuExpanded = false; onOpenDiagnostics() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.tools)) }, onClick = { topBarMenuExpanded = false; onOpenToolbox() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.developer_resources)) }, onClick = { topBarMenuExpanded = false; onOpenDeveloperResources() })
                        DropdownMenuItem(text = { Text(stringResource(R.string.settings)) }, onClick = { topBarMenuExpanded = false; onOpenSettings() })
                    }
                }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "engine-mode-switcher") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = false,
                        onClick = onOpenWebStudio,
                        label = { Text(stringResource(R.string.mode_browser)) },
                        leadingIcon = { Icon(Icons.Default.Web, contentDescription = null) }
                    )
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = { Text(stringResource(R.string.mode_api)) },
                        leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) }
                    )
                }
            }
            if (connectivityStatus != ConnectivityStatus.ONLINE) {
                item(key = "connectivity-banner") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (connectivityStatus == ConnectivityStatus.OFFLINE)
                                stringResource(R.string.network_offline_message)
                            else
                                stringResource(R.string.network_connecting_message),
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(stringResource(R.string.prompt_canvas), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = state.title, onValueChange = viewModel::updateTitle, label = { Text(stringResource(R.string.template_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.systemPrompt, onValueChange = viewModel::updateSystemPrompt, label = { Text(stringResource(R.string.system_instructions)) }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.userPrompt, onValueChange = viewModel::updateUserPrompt, label = { Text(stringResource(R.string.user_prompt)) }, minLines = 4, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.variablesText, onValueChange = viewModel::updateVariables, label = { Text(stringResource(R.string.dynamic_variables)) }, supportingText = { Text(stringResource(R.string.dynamic_variables_help)) }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        if (state.unresolvedVariables.isNotEmpty()) {
                            Text(stringResource(R.string.unresolved_prefix) + state.unresolvedVariables.joinToString { "{{" + it + "}}" }, color = MaterialTheme.colorScheme.error)
                        }
                        ModelSelector(
                            provider = state.provider,
                            modelId = state.modelId,
                            availableModels = state.availableModels,
                            availableModelsByProvider = state.availableModelsByProvider,
                            providerKeyHealth = state.providerKeyHealth,
                            lastFailedProvider = state.lastFailedProvider,
                            isLoadingModels = state.isLoadingModels,
                            isStreaming = state.isStreaming,
                            onOpen = {
                                viewModel.refreshProviderKeyHealth()
                                viewModel.refreshModels()
                            },
                            onSelectModel = { provider, modelId ->
                                if (state.provider != provider) viewModel.updateProvider(provider)
                                viewModel.updateModel(modelId)
                            },
                            onCancelStream = viewModel::cancelRun,
                            onManageKeys = onOpenSettings
                        )
                        Text(stringResource(R.string.saved_templates), style = MaterialTheme.typography.labelLarge)
                        if (state.savedTemplates.isEmpty()) Text(stringResource(R.string.save_prompt_offline), style = MaterialTheme.typography.bodySmall)
                        else state.savedTemplates.take(12).forEach { prompt ->
                            TextButton(onClick = { viewModel.loadTemplate(prompt) }) { Text(prompt.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::runPrompt, enabled = state.canRun, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null); Text(stringResource(R.string.run_prompt))
                            }
                            if (state.isStreaming) Button(onClick = viewModel::cancelRun) {
                                Icon(Icons.Default.Stop, contentDescription = null); Text(stringResource(R.string.stop))
                            } else TextButton(onClick = viewModel::saveTemplate) {
                                Icon(Icons.Default.Save, contentDescription = null); Text(stringResource(R.string.save))
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.token_cost_estimate), style = MaterialTheme.typography.titleMedium)
                        EstimateLine(stringResource(R.string.approx_input_tokens), state.estimate.inputTokens.toString())
                        EstimateLine(stringResource(R.string.approx_response_tokens), state.estimate.outputTokens.toString())
                        Text(stringResource(R.string.character_based_estimate), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = state.inputRateText, onValueChange = viewModel::updateInputRate, label = { Text(stringResource(R.string.input_rate)) }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = state.outputRateText, onValueChange = viewModel::updateOutputRate, label = { Text(stringResource(R.string.output_rate)) }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Text(stringResource(R.string.estimated_cost, state.estimate.inputCostUsd?.let { usd(it) } ?: stringResource(R.string.set_rate), state.estimate.outputCostUsd?.let { usd(it) } ?: stringResource(R.string.set_rate)))
                    }
                }
            }
            item(key = "response-stream") {
                // One response is streamed into this stable item; there is intentionally no token-level auto-scroll.
                // Keeping a key preserves the reader's viewport while markdown content updates in place.
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.response_stream), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (state.isStreaming) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(state.response)) }, enabled = state.response.isNotEmpty()) { Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_response)) }
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.response) }
                                if (state.response.isNotEmpty()) ContextCompat.startActivity(context, Intent.createChooser(send, exportChooserTitle), null)
                            }, enabled = state.response.isNotEmpty()) { Icon(Icons.Default.IosShare, contentDescription = stringResource(R.string.export_response_content_description)) }
                        }
                        HorizontalDivider()
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (state.response.isBlank() && !state.isStreaming) Text(stringResource(R.string.response_placeholder), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else SelectionContainer { MarkdownResponse(state.response) }
                    }
                }
            }
            if (state.recentRuns.isNotEmpty()) {
                item { Text(stringResource(R.string.recent_runs), style = MaterialTheme.typography.titleMedium) }
                items(state.recentRuns.take(5), key = { it.id }) { run ->
                    Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(run.promptTitle, fontWeight = FontWeight.SemiBold)
                        Text(run.provider.displayName + " • " + run.modelId, style = MaterialTheme.typography.bodySmall)
                        Text(run.responseText.take(180), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    } }
                }
            }
            item(key = "admob-banner-slot") {
                val showSponsor = !isOnline || bannerLoadFailed || !bannerLoaded || !canRequestAds || !adsReady
                Box(Modifier.fillMaxWidth().heightIn(min = 112.dp)) {
                    androidx.compose.animation.Crossfade(targetState = showSponsor, label = "sponsor-ad-fallback") { sponsor ->
                        if (sponsor) CoffeeSponsorCard(Modifier.fillMaxWidth())
                        else AdMobBanner(adMobManager = adMobManager, isOnline = isOnline, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun EstimateLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.SemiBold) }
}

private enum class MarkdownKind { TEXT, HEADING, BULLET, CODE }
private data class MarkdownBlock(val kind: MarkdownKind, val text: String)

private fun parseMarkdown(markdown: String): List<MarkdownBlock> {
    val result = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCode = false
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            result += MarkdownBlock(MarkdownKind.TEXT, paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }
    for (line in markdown.lines()) {
        if (line.trimStart().startsWith(CODE_FENCE)) {
            if (inCode) {
                result += MarkdownBlock(MarkdownKind.CODE, code.joinToString("\n"))
                code.clear()
                inCode = false
            } else {
                flushParagraph()
                inCode = true
            }
        } else if (inCode) {
            code += line
        } else if (line.isBlank()) {
            flushParagraph()
        } else if (line.trimStart().startsWith("#")) {
            flushParagraph()
            result += MarkdownBlock(MarkdownKind.HEADING, line.trimStart().trimStart('#').trim())
        } else if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            flushParagraph()
            result += MarkdownBlock(MarkdownKind.BULLET, line.trimStart().drop(2))
        } else {
            paragraph += line
        }
    }
    flushParagraph()
    if (code.isNotEmpty()) result += MarkdownBlock(MarkdownKind.CODE, code.joinToString("\n"))
    return result
}

@Composable
private fun MarkdownResponse(markdown: String) {
    val blocks = remember(markdown) { parseMarkdown(markdown) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block.kind) {
                MarkdownKind.HEADING -> Text(block.text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                MarkdownKind.BULLET -> Text("•  " + block.text, style = MaterialTheme.typography.bodyMedium)
                MarkdownKind.CODE -> Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest) {
                    Text(block.text, modifier = Modifier.fillMaxWidth().padding(12.dp), fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodySmall)
                }
                MarkdownKind.TEXT -> Text(block.text, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private const val CODE_FENCE = "\u0060\u0060\u0060"
private fun usd(value: Double): String = String.format(Locale.US, "$%.6f", value)
