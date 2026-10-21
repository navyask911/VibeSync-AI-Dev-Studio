package com.vibesync.promptstudio.presentation

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R

private enum class ResourceSection { PROMPTS, COMMANDS, PLAYBOOKS, SERVICES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperResourcesScreen(
    onBack: () -> Unit,
    onOpenWebStudio: () -> Unit,
    onSendPromptToBrowser: (String) -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenApiStudio: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenSettings: () -> Unit,
    showTopBar: Boolean = true
) {
    val clipboard = LocalClipboardManager.current
    val promptCopied = stringResource(R.string.developer_prompt_copied_feedback)
    val commandCopied = stringResource(R.string.developer_command_copied_feedback)
    val playbookCopied = stringResource(R.string.developer_playbook_copied_feedback)
    var section by remember { mutableStateOf(ResourceSection.PROMPTS) }
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<DeveloperPromptCategory?>(null) }
    var selectedPlatform by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    val promptResults = remember(search, selectedCategory) {
        DeveloperPromptCatalog.prompts.filter { p ->
            (selectedCategory == null || p.category == selectedCategory) && p.text.contains(search, ignoreCase = true)
        }
    }
    val commandResults = remember(search, selectedPlatform) {
        DeveloperCommandCatalog.commands.filter { c ->
            (selectedPlatform == null || c.platformRes == selectedPlatform) &&
                (c.title.contains(search, ignoreCase = true) || c.command.contains(search, ignoreCase = true))
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.developer_resources), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Image(painter = painterResource(R.drawable.ic_launcher), contentDescription = stringResource(R.string.app_name), modifier = Modifier.size(44.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                            Text(stringResource(R.string.app_brand_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.app_brand_subtitle), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                    Text(stringResource(R.string.developer_resources_hero), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.developer_resources_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(stringResource(R.string.developer_prompt_count, DeveloperPromptCatalog.prompts.size), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = section == ResourceSection.PROMPTS, onClick = { section = ResourceSection.PROMPTS; search = ""; feedback = null }, label = { Text(stringResource(R.string.developer_section_prompts)) }, leadingIcon = { Icon(Icons.Default.Code, contentDescription = null) })
                FilterChip(selected = section == ResourceSection.COMMANDS, onClick = { section = ResourceSection.COMMANDS; search = ""; feedback = null }, label = { Text(stringResource(R.string.developer_section_commands)) }, leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null) })
                FilterChip(selected = section == ResourceSection.PLAYBOOKS, onClick = { section = ResourceSection.PLAYBOOKS; search = ""; feedback = null }, label = { Text(stringResource(R.string.developer_section_playbooks)) }, leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) })
                FilterChip(selected = section == ResourceSection.SERVICES, onClick = { section = ResourceSection.SERVICES; search = ""; feedback = null }, label = { Text(stringResource(R.string.developer_section_services)) }, leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) })
            }
            when (section) {
                ResourceSection.PROMPTS -> {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), singleLine = true, label = { Text(stringResource(R.string.developer_prompt_search)) })
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(selected = selectedCategory == null, onClick = { selectedCategory = null }, label = { Text(stringResource(R.string.developer_prompt_category_all)) })
                        DeveloperPromptCategory.entries.forEach { category ->
                            FilterChip(selected = selectedCategory == category, onClick = { selectedCategory = if (selectedCategory == category) null else category }, label = { Text(stringResource(category.labelRes)) })
                        }
                    }
                    feedback?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(promptResults, key = { it.id }) { prompt ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(prompt.id.removePrefix("prompt-").uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(prompt.category.labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    SelectionContainer { Text(prompt.text, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { clipboard.setText(AnnotatedString(prompt.text)); feedback = promptCopied }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.developer_prompt_copy))
                                        }
                                        Button(onClick = { onSendPromptToBrowser(prompt.text) }) {
                                            Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.developer_prompt_send_to_browser))
                                        }
                                    }
                                }
                            }
                        }
                        if (promptResults.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp)) }
                    }
                }
                ResourceSection.COMMANDS -> {
                    OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), singleLine = true, label = { Text(stringResource(R.string.developer_command_search)) })
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        val platforms: List<Pair<Int?, Int>> = listOf(
                            null to R.string.developer_command_platform_all,
                            R.string.developer_command_platform_adb to R.string.developer_command_platform_adb,
                            R.string.developer_command_platform_powershell to R.string.developer_command_platform_powershell,
                            R.string.developer_command_platform_linux to R.string.developer_command_platform_linux,
                            R.string.developer_command_platform_git to R.string.developer_command_platform_git
                        )
                        platforms.forEach { (key, label) ->
                            FilterChip(selected = selectedPlatform == key, onClick = { selectedPlatform = key }, label = { Text(stringResource(label)) })
                        }
                    }
                    Text(stringResource(R.string.developer_command_safety_note), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    feedback?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(commandResults, key = { it.id }) { command ->
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(14.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(stringResource(command.platformRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text(command.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    SelectionContainer { Text(command.command, style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)) }
                                    TextButton(onClick = { clipboard.setText(AnnotatedString(command.command)); feedback = commandCopied }) {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(5.dp)); Text(stringResource(R.string.developer_prompt_copy))
                                    }
                                }
                            }
                        }
                        if (commandResults.isEmpty()) item { Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp)) }
                    }
                }
                ResourceSection.PLAYBOOKS -> {
                    Text(stringResource(R.string.developer_playbook_intro), modifier = Modifier.padding(horizontal = 16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    feedback?.let { Text(it, modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium) }
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(DeveloperPlaybookCatalog.playbooks, key = { it.id }) { playbook ->
                            val title = stringResource(playbook.titleRes)
                            val summary = stringResource(playbook.summaryRes)
                            val content = stringResource(playbook.contentRes)
                            val reviewPrompt = stringResource(R.string.developer_playbook_review_prompt, title, content)
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    SelectionContainer { Text(content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium) }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                        TextButton(onClick = { clipboard.setText(AnnotatedString(content)); feedback = playbookCopied }) {
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
                ResourceSection.SERVICES -> {
                    LazyColumn(modifier = Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        item { ServiceCard(stringResource(R.string.developer_service_offline_tools), stringResource(R.string.developer_service_offline_tools_description), { Icon(Icons.Default.Code, contentDescription = null) }, stringResource(R.string.developer_service_open), onOpenToolbox) }
                        item { ServiceCard(stringResource(R.string.developer_service_diagnostics), stringResource(R.string.developer_service_diagnostics_description), { Icon(Icons.Default.BugReport, contentDescription = null) }, stringResource(R.string.developer_service_open), onOpenDiagnostics) }
                        item { ServiceCard(stringResource(R.string.developer_service_api_studio), stringResource(R.string.developer_service_api_studio_description), { Icon(Icons.Default.Terminal, contentDescription = null) }, stringResource(R.string.developer_service_open), onOpenApiStudio) }
                        item { ServiceCard(stringResource(R.string.developer_service_web_studio), stringResource(R.string.developer_service_web_studio_description), { Icon(Icons.Default.Web, contentDescription = null) }, stringResource(R.string.developer_service_open), onOpenWebStudio) }
                        item { ServiceCard(stringResource(R.string.developer_service_appearance), stringResource(R.string.developer_service_appearance_description), { Icon(Icons.Default.Settings, contentDescription = null) }, stringResource(R.string.developer_service_open), onOpenSettings) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceCard(title: String, description: String, icon: @Composable () -> Unit, buttonLabel: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { icon(); Spacer(Modifier.width(8.dp)); Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) }
            Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onClick) { Text(buttonLabel) }
        }
    }
}
