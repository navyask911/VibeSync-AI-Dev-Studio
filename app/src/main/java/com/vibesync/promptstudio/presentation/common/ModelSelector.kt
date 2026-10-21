package com.vibesync.promptstudio.presentation.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.presentation.ProviderKeyHealth
import java.util.Locale

@Composable
fun ModelSelector(
    provider: ProviderType,
    modelId: String,
    availableModels: List<String>,
    providerKeyHealth: Map<ProviderType, ProviderKeyHealth>,
    lastFailedProvider: ProviderType?,
    isLoadingModels: Boolean,
    isStreaming: Boolean,
    onOpen: () -> Unit,
    onSelectModel: (ProviderType, String) -> Unit,
    onCancelStream: () -> Unit,
    onManageKeys: () -> Unit
) {
    var sheetVisible by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf(ModelFilter.ALL) }
    var pendingSelection by remember { mutableStateOf<ModelOption?>(null) }
    var confirmCancellation by remember { mutableStateOf(false) }
    val selected = remember(provider, modelId) { modelOption(provider, modelId) }
    val streamingLabel = androidx.compose.ui.res.stringResource(R.string.model_streaming_supported)
    val unknownPriceLabel = androidx.compose.ui.res.stringResource(R.string.model_price_unknown)

    Surface(
        modifier = Modifier.fillMaxWidth().clickable {
            sheetVisible = true
            onOpen()
        },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(providerSymbol(provider), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.size(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(modelId, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(provider.displayName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Default.ArrowDropDown, contentDescription = androidx.compose.ui.res.stringResource(R.string.model_options), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                compactMetadata(selected, streamingLabel),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }

    if (sheetVisible) {
        val options = remember(availableModels, provider, modelId) {
            ProviderType.entries.flatMap { optionProvider ->
                val discovered = if (optionProvider == provider) availableModels + modelId else emptyList()
                (optionProvider.modelOptions + discovered)
                    .filter { it.isNotBlank() }
                    .distinct()
                    .map { modelOption(optionProvider, it) }
            }
        }
        ModalBottomSheet(onDismissRequest = { sheetVisible = false }) {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 720.dp).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    androidx.compose.ui.res.stringResource(R.string.model_selector_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = filter == ModelFilter.ALL, onClick = { filter = ModelFilter.ALL }, label = { Text(androidx.compose.ui.res.stringResource(R.string.model_filter_all)) })
                    FilterChip(selected = filter == ModelFilter.FAST, onClick = { filter = ModelFilter.FAST }, label = { Text(androidx.compose.ui.res.stringResource(R.string.model_filter_fast)) })
                    FilterChip(selected = filter == ModelFilter.CHEAP, onClick = { filter = ModelFilter.CHEAP }, label = { Text(androidx.compose.ui.res.stringResource(R.string.model_filter_cheap)) })
                    FilterChip(selected = filter == ModelFilter.CAPABLE, onClick = { filter = ModelFilter.CAPABLE }, label = { Text(androidx.compose.ui.res.stringResource(R.string.model_filter_capable)) })
                }
                if (isLoadingModels) LinearProgressIndicator(Modifier.fillMaxWidth())
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    ProviderType.entries.forEach { optionProvider ->
                        val group = options.filter { it.provider == optionProvider && matchesFilter(it, filter) }
                        if (group.isNotEmpty()) {
                            item(key = "provider-header-${optionProvider.name}") {
                                Text(
                                    optionProvider.displayName,
                                    modifier = Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(group, key = { "${it.provider.name}:${it.modelId}" }) { option ->
                                val keyHealth = when {
                                    lastFailedProvider == option.provider -> ProviderKeyHealth.ERROR
                                    else -> providerKeyHealth[option.provider] ?: ProviderKeyHealth.UNKNOWN
                                }
                                val (healthColor, healthLabel) = when (keyHealth) {
                                    ProviderKeyHealth.PRESENT -> MaterialTheme.colorScheme.primary to androidx.compose.ui.res.stringResource(R.string.model_key_health_ready)
                                    ProviderKeyHealth.MISSING -> Color(0xFFFFB300) to androidx.compose.ui.res.stringResource(R.string.model_key_health_missing)
                                    ProviderKeyHealth.ERROR -> MaterialTheme.colorScheme.error to androidx.compose.ui.res.stringResource(R.string.model_key_health_error)
                                    ProviderKeyHealth.UNKNOWN -> MaterialTheme.colorScheme.outline to androidx.compose.ui.res.stringResource(R.string.model_key_health_unknown)
                                }
                                Surface(
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        if (option.provider == provider && option.modelId == modelId) {
                                            sheetVisible = false
                                        } else if (isStreaming) {
                                            pendingSelection = option
                                            confirmCancellation = true
                                        } else {
                                            onSelectModel(option.provider, option.modelId)
                                            sheetVisible = false
                                        }
                                    },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (option.provider == provider && option.modelId == modelId)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceContainer,
                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(
                                                Modifier.size(9.dp).background(healthColor, CircleShape)
                                                    .semantics { contentDescription = healthLabel }
                                            )
                                            Text(
                                                option.modelId,
                                                modifier = Modifier.weight(1f),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            option.capabilities.forEach { capability ->
                                                Badge(
                                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                                ) {
                                                    Text(androidx.compose.ui.res.stringResource(capability), modifier = Modifier.padding(horizontal = 3.dp, vertical = 1.dp))
                                                }
                                            }
                                        }
                                        Text(
                                            priceLine(option, unknownPriceLabel),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            compactMetadata(option, streamingLabel),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                TextButton(onClick = { sheetVisible = false; onManageKeys() }, modifier = Modifier.fillMaxWidth()) {
                    Text(androidx.compose.ui.res.stringResource(R.string.model_manage_providers_keys))
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    if (confirmCancellation) {
        AlertDialog(
            onDismissRequest = { confirmCancellation = false; pendingSelection = null },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.model_switch_stream_title)) },
            text = { Text(androidx.compose.ui.res.stringResource(R.string.model_switch_stream_message)) },
            confirmButton = {
                Button(onClick = {
                    val selection = pendingSelection
                    confirmCancellation = false
                    pendingSelection = null
                    onCancelStream()
                    selection?.let { onSelectModel(it.provider, it.modelId) }
                    sheetVisible = false
                }) { Text(androidx.compose.ui.res.stringResource(R.string.model_switch_stream_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmCancellation = false; pendingSelection = null }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.cancel))
                }
            }
        )
    }
}

private enum class ModelFilter { ALL, FAST, CHEAP, CAPABLE }

private data class ModelOption(
    val provider: ProviderType,
    val modelId: String,
    val context: String,
    val inputUsd: Double?,
    val outputUsd: Double?,
    val capabilities: List<Int>,
    val fast: Boolean = false,
    val cheap: Boolean = false,
    val capable: Boolean = false
)

private fun providerSymbol(provider: ProviderType): String = when (provider) {
    ProviderType.OPENAI -> "◎"
    ProviderType.ANTHROPIC -> "✦"
    ProviderType.GEMINI -> "✧"
}

private fun modelOption(provider: ProviderType, modelId: String): ModelOption =
    when ("${provider.name}:$modelId") {
        // Input/output prices are USD per million tokens; unknown provider/model rates stay unknown.
        "OPENAI:gpt-4o" -> ModelOption(provider, modelId, "128k", null, null, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), capable = true)
        "OPENAI:gpt-4o-mini" -> ModelOption(provider, modelId, "128k", null, null, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), fast = true)
        "OPENAI:gpt-4.1" -> ModelOption(provider, modelId, "1M", null, null, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), capable = true)
        "ANTHROPIC:claude-sonnet-4-5" -> ModelOption(provider, modelId, "200k", 3.00, 15.00, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), fast = true, capable = true)
        "ANTHROPIC:claude-opus-4-1" -> ModelOption(provider, modelId, "200k", 15.00, 75.00, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json, R.string.model_capability_reasoning), capable = true)
        "ANTHROPIC:claude-haiku-4-5" -> ModelOption(provider, modelId, "200k", 1.00, 5.00, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), fast = true, cheap = true)
        "GEMINI:gemini-2.5-flash" -> ModelOption(provider, modelId, "1M", 0.30, 2.50, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json), fast = true, cheap = true)
        "GEMINI:gemini-2.5-pro" -> ModelOption(provider, modelId, "1M", 1.25, 10.00, listOf(R.string.model_capability_tools, R.string.model_capability_vision, R.string.model_capability_json, R.string.model_capability_reasoning), capable = true)
        else -> ModelOption(provider, modelId, "—", null, null, listOf(R.string.model_streaming_supported))
    }

private fun matchesFilter(model: ModelOption, filter: ModelFilter): Boolean = when (filter) {
    ModelFilter.ALL -> true
    ModelFilter.FAST -> model.fast
    ModelFilter.CHEAP -> model.cheap
    ModelFilter.CAPABLE -> model.capable
}

private fun priceLine(model: ModelOption, unknownPriceLabel: String): String =
    if (model.inputUsd == null || model.outputUsd == null) "$— / $— · $unknownPriceLabel"
    else "Input ${usd(model.inputUsd)} · Output ${usd(model.outputUsd)} / 1M tokens"

private fun compactMetadata(model: ModelOption, streamingLabel: String): String {
    val price = if (model.inputUsd == null || model.outputUsd == null) "—" else "$" + usdNumber(model.inputUsd) + "/$" + usdNumber(model.outputUsd)
    return "${model.context} ctx · $price · $streamingLabel"
}

private fun usd(value: Double): String = "$" + usdNumber(value)
private fun usdNumber(value: Double): String = String.format(Locale.US, "%.2f", value)
