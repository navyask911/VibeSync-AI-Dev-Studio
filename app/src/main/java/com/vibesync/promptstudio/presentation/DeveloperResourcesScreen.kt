package com.vibesync.promptstudio.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
    val context = LocalContext.current
    val promptCopied = stringResource(R.string.developer_prompt_copied_feedback)
    val commandCopied = stringResource(R.string.developer_command_copied_feedback)
    val playbookCopied = stringResource(R.string.developer_playbook_copied_feedback)
    var section by remember { mutableStateOf(ResourceSection.PROMPTS) }
    var search by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<DeveloperPromptCategory?>(null) }
    var selectedPlatform by remember { mutableStateOf<Int?>(null) }
    var feedback by remember { mutableStateOf<String?>(null) }
    var expandedPromptId by remember { mutableStateOf<String?>(null) }
    var expandedCommandId by remember { mutableStateOf<String?>(null) }
    var expandedPlaybookId by remember { mutableStateOf<String?>(null) }
    var expandedServiceId by remember { mutableStateOf<String?>(null) }

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
    val playbookResults = remember(search, context) {
        DeveloperPlaybookCatalog.playbooks.filter { playbook ->
            search.isBlank() ||
                context.getString(playbook.titleRes).contains(search, ignoreCase = true) ||
                context.getString(playbook.summaryRes).contains(search, ignoreCase = true) ||
                context.getString(playbook.contentRes).contains(search, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            if (showTopBar) CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.developer_resources),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.developer_prompt_count, DeveloperPromptCatalog.prompts.size),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        showSearch = !showSearch
                        if (!showSearch) search = ""
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = if (showSearch) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = stringResource(
                            if (showSearch) R.string.developer_search_close else R.string.developer_search_toggle
                        )
                    )
                }
            }

            AnimatedVisibility(
                visible = showSearch,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    singleLine = true,
                    label = {
                        Text(
                            stringResource(
                                when (section) {
                                    ResourceSection.PROMPTS -> R.string.developer_prompt_search
                                    ResourceSection.COMMANDS -> R.string.developer_command_search
                                    else -> R.string.developer_search_toggle
                                }
                            )
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = section == ResourceSection.PROMPTS,
                    onClick = { section = ResourceSection.PROMPTS; feedback = null },
                    label = { Text(stringResource(R.string.developer_section_prompts)) },
                    leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = section == ResourceSection.COMMANDS,
                    onClick = { section = ResourceSection.COMMANDS; feedback = null },
                    label = { Text(stringResource(R.string.developer_section_commands)) },
                    leadingIcon = { Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = section == ResourceSection.PLAYBOOKS,
                    onClick = { section = ResourceSection.PLAYBOOKS; feedback = null },
                    label = { Text(stringResource(R.string.developer_section_playbooks)) },
                    leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
                FilterChip(
                    selected = section == ResourceSection.SERVICES,
                    onClick = { section = ResourceSection.SERVICES; feedback = null },
                    label = { Text(stringResource(R.string.developer_section_services)) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }

            when (section) {
                ResourceSection.PROMPTS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.developer_prompt_category_all)) }
                        )
                        DeveloperPromptCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = {
                                    selectedCategory = if (selectedCategory == category) null else category
                                },
                                label = { Text(stringResource(category.labelRes)) }
                            )
                        }
                    }
                    feedback?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(promptResults, key = { it.id }) { prompt ->
                            val expanded = expandedPromptId == prompt.id
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .animateContentSize()
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(
                                            if (expanded) R.string.developer_accordion_collapse
                                            else R.string.developer_accordion_expand
                                        ),
                                        onClick = { expandedPromptId = if (expanded) null else prompt.id }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                prompt.id.removePrefix("prompt-").uppercase(),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                stringResource(prompt.category.labelRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(
                                                if (expanded) R.string.developer_accordion_collapse
                                                else R.string.developer_accordion_expand
                                            )
                                        )
                                    }
                                    if (!expanded) {
                                        Text(
                                            prompt.text,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SelectionContainer {
                                                Text(prompt.text, style = MaterialTheme.typography.bodyMedium)
                                            }
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                TextButton(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(prompt.text))
                                                        feedback = promptCopied
                                                    }
                                                ) {
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
                        if (promptResults.isEmpty()) item {
                            Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                ResourceSection.COMMANDS -> {
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val platforms: List<Pair<Int?, Int>> = listOf(
                            null to R.string.developer_command_platform_all,
                            R.string.developer_command_platform_adb to R.string.developer_command_platform_adb,
                            R.string.developer_command_platform_powershell to R.string.developer_command_platform_powershell,
                            R.string.developer_command_platform_linux to R.string.developer_command_platform_linux,
                            R.string.developer_command_platform_git to R.string.developer_command_platform_git
                        )
                        platforms.forEach { (key, label) ->
                            FilterChip(
                                selected = selectedPlatform == key,
                                onClick = { selectedPlatform = key },
                                label = { Text(stringResource(label)) }
                            )
                        }
                    }
                    Text(
                        stringResource(R.string.developer_command_safety_note),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    feedback?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(commandResults, key = { it.id }) { command ->
                            val expanded = expandedCommandId == command.id
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .animateContentSize()
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(
                                            if (expanded) R.string.developer_accordion_collapse
                                            else R.string.developer_accordion_expand
                                        ),
                                        onClick = { expandedCommandId = if (expanded) null else command.id }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                command.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                stringResource(command.platformRes),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(
                                                if (expanded) R.string.developer_accordion_collapse
                                                else R.string.developer_accordion_expand
                                            )
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SelectionContainer {
                                                Text(
                                                    command.command,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                                                )
                                            }
                                            TextButton(
                                                onClick = {
                                                    clipboard.setText(AnnotatedString(command.command))
                                                    feedback = commandCopied
                                                }
                                            ) {
                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text(stringResource(R.string.developer_prompt_copy))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (commandResults.isEmpty()) item {
                            Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                ResourceSection.PLAYBOOKS -> {
                    Text(
                        stringResource(R.string.developer_playbook_intro),
                        modifier = Modifier.padding(horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    feedback?.let {
                        Text(
                            it,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(playbookResults, key = { it.id }) { playbook ->
                            val title = stringResource(playbook.titleRes)
                            val summary = stringResource(playbook.summaryRes)
                            val content = stringResource(playbook.contentRes)
                            val reviewPrompt = stringResource(R.string.developer_playbook_review_prompt, title, content)
                            val expanded = expandedPlaybookId == playbook.id
                            Card(
                                modifier = Modifier.fillMaxWidth()
                                    .animateContentSize()
                                    .clickable(
                                        role = Role.Button,
                                        onClickLabel = stringResource(
                                            if (expanded) R.string.developer_accordion_collapse
                                            else R.string.developer_accordion_expand
                                        ),
                                        onClick = { expandedPlaybookId = if (expanded) null else playbook.id }
                                    ),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Spacer(Modifier.width(8.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(
                                            imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = stringResource(
                                                if (expanded) R.string.developer_accordion_collapse
                                                else R.string.developer_accordion_expand
                                            )
                                        )
                                    }
                                    AnimatedVisibility(
                                        visible = expanded,
                                        enter = expandVertically() + fadeIn(),
                                        exit = shrinkVertically() + fadeOut()
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                            SelectionContainer {
                                                Text(content, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                                TextButton(
                                                    onClick = {
                                                        clipboard.setText(AnnotatedString(content))
                                                        feedback = playbookCopied
                                                    }
                                                ) {
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
                        if (playbookResults.isEmpty()) item {
                            Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp))
                        }
                    }
                }

                ResourceSection.SERVICES -> {
                    val services = listOf(
                        Triple(R.string.developer_service_offline_tools, R.string.developer_service_offline_tools_description, 0),
                        Triple(R.string.developer_service_diagnostics, R.string.developer_service_diagnostics_description, 1),
                        Triple(R.string.developer_service_api_studio, R.string.developer_service_api_studio_description, 2),
                        Triple(R.string.developer_service_web_studio, R.string.developer_service_web_studio_description, 3),
                        Triple(R.string.developer_service_appearance, R.string.developer_service_appearance_description, 4)
                    ).filter { (titleRes, descriptionRes) ->
                        search.isBlank() ||
                            context.getString(titleRes).contains(search, ignoreCase = true) ||
                            context.getString(descriptionRes).contains(search, ignoreCase = true)
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(services, key = { it.third }) { (titleRes, descriptionRes, id) ->
                            val icon: @Composable () -> Unit = when (id) {
                                0 -> { { Icon(Icons.Default.Code, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                                1 -> { { Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                                2 -> { { Icon(Icons.Default.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                                3 -> { { Icon(Icons.Default.Web, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                                else -> { { Icon(Icons.Default.Settings, contentDescription = null, tint = MaterialTheme.colorScheme.primary) } }
                            }
                            val onOpen = when (id) {
                                0 -> onOpenToolbox
                                1 -> onOpenDiagnostics
                                2 -> onOpenApiStudio
                                3 -> onOpenWebStudio
                                else -> onOpenSettings
                            }
                            ServiceAccordionCard(
                                title = stringResource(titleRes),
                                description = stringResource(descriptionRes),
                                icon = icon,
                                buttonLabel = stringResource(R.string.developer_service_open),
                                expanded = expandedServiceId == id.toString(),
                                onToggle = {
                                    expandedServiceId = if (expandedServiceId == id.toString()) null else id.toString()
                                },
                                onOpen = onOpen
                            )
                        }
                        if (services.isEmpty()) item {
                            Text(stringResource(R.string.developer_no_results), modifier = Modifier.padding(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ServiceAccordionCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    buttonLabel: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
            .animateContentSize()
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(
                    if (expanded) R.string.developer_accordion_collapse
                    else R.string.developer_accordion_expand
                ),
                onClick = onToggle
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = stringResource(
                        if (expanded) R.string.developer_accordion_collapse
                        else R.string.developer_accordion_expand
                    )
                )
            }
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onOpen) { Text(buttonLabel) }
                }
            }
        }
    }
}
