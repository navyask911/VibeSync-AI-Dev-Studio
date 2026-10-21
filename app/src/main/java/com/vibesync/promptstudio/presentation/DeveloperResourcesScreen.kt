package com.vibesync.promptstudio.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R

private enum class ResourceSection { PROMPTS, COMMANDS, PLAYBOOKS, SERVICES }

private data class ResourceService(
    val id: String,
    val titleRes: Int,
    val descriptionRes: Int,
    val onOpen: () -> Unit
)

private data class PlaybookUi(
    val item: DeveloperPlaybook,
    val title: String,
    val summary: String,
    val content: String
)

@Composable
fun DeveloperResourcesScreen(
    onBack: () -> Unit,
    onOpenWebStudio: () -> Unit,
    onSendPromptToBrowser: (String) -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenApiStudio: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    initialSection: String = "prompts"
) {
    val clipboard = LocalClipboardManager.current
    val promptCopied = stringResource(R.string.developer_prompt_copied_feedback)
    val commandCopied = stringResource(R.string.developer_command_copied_feedback)
    val playbookCopied = stringResource(R.string.developer_playbook_copied_feedback)
    var section by remember(initialSection) {
        mutableStateOf(
            when (initialSection) {
                "commands" -> ResourceSection.COMMANDS
                "playbooks" -> ResourceSection.PLAYBOOKS
                "services" -> ResourceSection.SERVICES
                else -> ResourceSection.PROMPTS
            }
        )
    }
    var searchExpanded by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DeveloperPromptCategory?>(null) }
    var selectedPlatform by remember { mutableStateOf<Int?>(null) }
    var expandedId by remember { mutableStateOf<String?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val services = listOf(
        ResourceService("toolbox", R.string.developer_service_offline_tools, R.string.developer_service_offline_tools_description, onOpenToolbox),
        ResourceService("diagnostics", R.string.developer_service_diagnostics, R.string.developer_service_diagnostics_description, onOpenDiagnostics),
        ResourceService("api", R.string.developer_service_api_studio, R.string.developer_service_api_studio_description, onOpenApiStudio),
        ResourceService("web", R.string.developer_service_web_studio, R.string.developer_service_web_studio_description, onOpenWebStudio),
        ResourceService("appearance", R.string.developer_service_appearance, R.string.developer_service_appearance_description, onOpenSettings)
    )
    val serviceUi = services.map { service ->
        Triple(service, stringResource(service.titleRes), stringResource(service.descriptionRes))
    }
    val playbooks = DeveloperPlaybookCatalog.playbooks.map { item ->
        PlaybookUi(
            item = item,
            title = stringResource(item.titleRes),
            summary = stringResource(item.summaryRes),
            content = stringResource(item.contentRes)
        )
    }
    val promptResults = remember(search, selectedCategory) {
        DeveloperPromptCatalog.prompts.filter { prompt ->
            (selectedCategory == null || prompt.category == selectedCategory) &&
                prompt.text.contains(search, ignoreCase = true)
        }
    }
    val commandResults = remember(search, selectedPlatform) {
        DeveloperCommandCatalog.commands.filter { command ->
            (selectedPlatform == null || command.platformRes == selectedPlatform) &&
                (command.title.contains(search, ignoreCase = true) ||
                    command.command.contains(search, ignoreCase = true))
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    stringResource(R.string.developer_resources),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.developer_prompt_count, DeveloperPromptCatalog.prompts.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = {
                    searchExpanded = !searchExpanded
                    if (!searchExpanded) search = ""
                }
            ) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = stringResource(
                        if (searchExpanded) R.string.developer_search_hide else R.string.developer_search_show
                    )
                )
            }
        }
        AnimatedVisibility(visible = searchExpanded) {
            OutlinedTextField(
                value = search,
                onValueChange = { search = it; expandedId = null },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.developer_resources_search)) }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = section == ResourceSection.PROMPTS,
                onClick = { section = ResourceSection.PROMPTS; expandedId = null; feedback = null },
                label = { Text(stringResource(R.string.developer_section_prompts)) },
                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = section == ResourceSection.COMMANDS,
                onClick = { section = ResourceSection.COMMANDS; expandedId = null; feedback = null },
                label = { Text(stringResource(R.string.developer_section_commands)) },
                leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = section == ResourceSection.PLAYBOOKS,
                onClick = { section = ResourceSection.PLAYBOOKS; expandedId = null; feedback = null },
                label = { Text(stringResource(R.string.developer_section_playbooks)) },
                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
            FilterChip(
                selected = section == ResourceSection.SERVICES,
                onClick = { section = ResourceSection.SERVICES; expandedId = null; feedback = null },
                label = { Text(stringResource(R.string.developer_section_services)) },
                leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
            )
        }

        if (section == ResourceSection.PROMPTS) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null; expandedId = null },
                    label = { Text(stringResource(R.string.developer_prompt_category_all)) }
                )
                DeveloperPromptCategory.entries.forEach { category ->
                    FilterChip(
                        selected = selectedCategory == category,
                        onClick = { selectedCategory = if (selectedCategory == category) null else category; expandedId = null },
                        label = { Text(stringResource(category.labelRes)) }
                    )
                }
            }
        } else if (section == ResourceSection.COMMANDS) {
            val platforms = listOf(
                null to R.string.developer_command_platform_all,
                R.string.developer_command_platform_adb to R.string.developer_command_platform_adb,
                R.string.developer_command_platform_powershell to R.string.developer_command_platform_powershell,
                R.string.developer_command_platform_linux to R.string.developer_command_platform_linux,
                R.string.developer_command_platform_git to R.string.developer_command_platform_git
            )
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                platforms.forEach { (key, labelRes) ->
                    FilterChip(
                        selected = selectedPlatform == key,
                        onClick = { selectedPlatform = key; expandedId = null },
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }
        }

        feedback?.let {
            Text(
                text = it,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(top = 3.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (section) {
                ResourceSection.PROMPTS -> {
                    items(promptResults, key = { it.id }) { prompt ->
                        val expanded = expandedId == prompt.id
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            onClick = { expandedId = if (expanded) null else prompt.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            prompt.id.removePrefix("prompt-").uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            stringResource(prompt.category.labelRes),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                        SelectionContainer {
                                            Text(prompt.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        }
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { clipboard.setText(AnnotatedString(prompt.text)); feedback = promptCopied }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(stringResource(R.string.developer_prompt_copy))
                                            }
                                            Button(onClick = { onSendPromptToBrowser(prompt.text) }) {
                                                Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(stringResource(R.string.developer_prompt_send_to_browser))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (promptResults.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(12.dp)) }
                }
                ResourceSection.COMMANDS -> {
                    items(commandResults, key = { it.id }) { command ->
                        val expanded = expandedId == command.id
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            onClick = { expandedId = if (expanded) null else command.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(stringResource(command.platformRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Text(command.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        SelectionContainer {
                                            Text(command.command, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                                        }
                                        TextButton(onClick = { clipboard.setText(AnnotatedString(command.command)); feedback = commandCopied }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text(stringResource(R.string.developer_prompt_copy))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (commandResults.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(12.dp)) }
                    item {
                        Text(
                            stringResource(R.string.developer_command_safety_note),
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                ResourceSection.PLAYBOOKS -> {
                    val filtered = playbooks.filter { card ->
                        search.isBlank() || card.title.contains(search, true) || card.summary.contains(search, true)
                    }
                    item {
                        Text(
                            stringResource(R.string.developer_playbook_intro),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                    items(filtered, key = { it.item.id }) { card ->
                        val item = card.item
                        val expanded = expandedId == item.id
                        val reviewPrompt = stringResource(R.string.developer_playbook_review_prompt, card.title, card.content)
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            onClick = { expandedId = if (expanded) null else item.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(card.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Text(card.summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        SelectionContainer { Text(card.content, style = MaterialTheme.typography.bodyMedium) }
                                        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                            TextButton(onClick = { clipboard.setText(AnnotatedString(card.content)); feedback = playbookCopied }) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(stringResource(R.string.developer_playbook_copy))
                                            }
                                            Button(onClick = { onSendPromptToBrowser(reviewPrompt) }) {
                                                Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(stringResource(R.string.developer_playbook_review))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(12.dp)) }
                }
                ResourceSection.SERVICES -> {
                    val filtered = serviceUi.filter { (_, title, description) ->
                        search.isBlank() || title.contains(search, true) || description.contains(search, true)
                    }
                    items(filtered, key = { it.first.id }) { (service, title, description) ->
                        val expanded = expandedId == service.id
                        Card(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            onClick = { expandedId = if (expanded) null else service.id },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        when (service.id) {
                                            "diagnostics" -> Icons.Default.BugReport
                                            "appearance" -> Icons.Default.Settings
                                            "web" -> Icons.Default.Web
                                            "toolbox" -> Icons.Default.Code
                                            else -> Icons.Default.Terminal
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                }
                                AnimatedVisibility(visible = expanded) {
                                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                                        Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        TextButton(onClick = service.onOpen) { Text(stringResource(R.string.developer_service_open)) }
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(12.dp)) }
                }
            }
        }
    }
}
