package com.vibesync.promptstudio.presentation

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.BugReport
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
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.data.diagnostics.ConnectivityStatus
import com.vibesync.promptstudio.data.ads.AdMobManager
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptStudioScreen(
    viewModel: PromptStudioViewModel,
    onOpenSettings: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    connectivityStatus: ConnectivityStatus,
    adMobManager: AdMobManager,
    onSuccessfulPromptRun: (String) -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var lastHandledSuccess by remember { mutableIntStateOf(state.successfulRunCount) }
    LaunchedEffect(state.successfulRunCount) {
        if (state.successfulRunCount > lastHandledSuccess) onSuccessfulPromptRun(state.provider.displayName)
        lastHandledSuccess = state.successfulRunCount
    }
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("VibeSync Prompt Studio") },
            actions = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = onOpenDiagnostics) {
                        Icon(Icons.Default.BugReport, contentDescription = null)
                        Text("Diagnostics")
                    }
                    IconButton(onClick = onOpenSettings) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                }
            }
        )
    }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (connectivityStatus != ConnectivityStatus.ONLINE) {
                item(key = "connectivity-banner") {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            if (connectivityStatus == ConnectivityStatus.OFFLINE)
                                "You're offline. Prompt editing, saved templates, and local history still work; remote AI requests need internet access."
                            else
                                "Internet access is not yet validated. Local prompt editing works; remote AI requests may fail until internet connectivity is available.",
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
                        Text("Prompt canvas", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(value = state.title, onValueChange = viewModel::updateTitle, label = { Text("Template name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.systemPrompt, onValueChange = viewModel::updateSystemPrompt, label = { Text("System instructions") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.userPrompt, onValueChange = viewModel::updateUserPrompt, label = { Text("User prompt") }, minLines = 4, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = state.variablesText, onValueChange = viewModel::updateVariables, label = { Text("Dynamic variables (name=value per line)") }, supportingText = { Text("Use {{variable_name}}. Values stay on-device.") }, minLines = 2, modifier = Modifier.fillMaxWidth())
                        if (state.unresolvedVariables.isNotEmpty()) {
                            Text("Unresolved: " + state.unresolvedVariables.joinToString { "{{" + it + "}}" }, color = MaterialTheme.colorScheme.error)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ProviderPicker(state.provider.displayName) { label ->
                                viewModel.updateProvider(ProviderType.entries.firstOrNull { it.displayName == label } ?: state.provider)
                            }
                            OutlinedTextField(value = state.modelId, onValueChange = viewModel::updateModel, label = { Text("Model ID") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Text("Saved templates", style = MaterialTheme.typography.labelLarge)
                        if (state.savedTemplates.isEmpty()) Text("Save a prompt to keep it available offline.", style = MaterialTheme.typography.bodySmall)
                        else state.savedTemplates.take(12).forEach { prompt ->
                            TextButton(onClick = { viewModel.loadTemplate(prompt) }) { Text(prompt.title, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = viewModel::runPrompt, enabled = state.canRun, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null); Text("Run prompt")
                            }
                            if (state.isStreaming) Button(onClick = viewModel::cancelRun) {
                                Icon(Icons.Default.Stop, contentDescription = null); Text("Stop")
                            } else TextButton(onClick = viewModel::saveTemplate) {
                                Icon(Icons.Default.Save, contentDescription = null); Text("Save")
                            }
                        }
                    }
                }
            }
            item {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Live token & cost estimate", style = MaterialTheme.typography.titleMedium)
                        EstimateLine("Approx. input tokens", state.estimate.inputTokens.toString())
                        EstimateLine("Approx. response tokens", state.estimate.outputTokens.toString())
                        Text("Character-based estimate; exact counts require the provider tokenizer.", style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = state.inputRateText, onValueChange = viewModel::updateInputRate, label = { Text("Input $/1M") }, singleLine = true, modifier = Modifier.weight(1f))
                            OutlinedTextField(value = state.outputRateText, onValueChange = viewModel::updateOutputRate, label = { Text("Output $/1M") }, singleLine = true, modifier = Modifier.weight(1f))
                        }
                        Text("Estimated input cost: " + (state.estimate.inputCostUsd?.let { usd(it) } ?: "set rate") + " • output: " + (state.estimate.outputCostUsd?.let { usd(it) } ?: "set rate"))
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Response stream", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            if (state.isStreaming) CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                            IconButton(onClick = { clipboard.setText(AnnotatedString(state.response)) }, enabled = state.response.isNotEmpty()) { Icon(Icons.Default.ContentCopy, contentDescription = "Copy") }
                            IconButton(onClick = {
                                val send = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, state.response) }
                                if (state.response.isNotEmpty()) ContextCompat.startActivity(context, Intent.createChooser(send, "Export response"), null)
                            }, enabled = state.response.isNotEmpty()) { Icon(Icons.Default.IosShare, contentDescription = "Export") }
                        }
                        HorizontalDivider()
                        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                        if (state.response.isBlank() && !state.isStreaming) Text("Your answer will stream here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        else SelectionContainer { MarkdownResponse(state.response) }
                    }
                }
            }
            if (state.recentRuns.isNotEmpty()) {
                item { Text("Recent runs", style = MaterialTheme.typography.titleMedium) }
                items(state.recentRuns.take(5), key = { it.id }) { run ->
                    Card { Column(Modifier.fillMaxWidth().padding(12.dp)) {
                        Text(run.promptTitle, fontWeight = FontWeight.SemiBold)
                        Text(run.provider.displayName + " • " + run.modelId, style = MaterialTheme.typography.bodySmall)
                        Text(run.responseText.take(180), maxLines = 3, overflow = TextOverflow.Ellipsis)
                    } }
                }
            }
            item(key = "admob-banner-slot") {
                AdMobBanner(adMobManager = adMobManager, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EstimateLine(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(label); Text(value, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun ProviderPicker(selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Text("Provider", style = MaterialTheme.typography.labelSmall)
        TextButton(onClick = { expanded = true }) { Text(selected) }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ProviderType.entries.forEach { provider ->
                DropdownMenuItem(text = { Text(provider.displayName) }, onClick = { onSelected(provider.displayName); expanded = false })
            }
        }
    }
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
