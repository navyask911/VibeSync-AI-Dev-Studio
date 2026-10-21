package com.vibesync.promptstudio.presentation
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.domain.usecase.DeveloperToolboxTools
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ToolboxEntry(
    val id: String,
    val nameRes: Int,
    val categoryId: String,
    val descriptionRes: Int,
    val inputLabelRes: Int,
    val inputHintRes: Int,
    val secondLabelRes: Int? = null,
    val secondHintRes: Int? = null
)

private data class ToolboxCategory(val id: String, val labelRes: Int)

private val toolboxEntries = listOf(
    ToolboxEntry("json", R.string.toolbox_json_name, "data_encoding", R.string.toolbox_json_description, R.string.toolbox_json_input_label, R.string.toolbox_json_input_hint),
    ToolboxEntry("xml", R.string.toolbox_xml_name, "data_encoding", R.string.toolbox_xml_description, R.string.toolbox_xml_input_label, R.string.toolbox_xml_input_hint),
    ToolboxEntry("diff", R.string.toolbox_diff_name, "text_regex", R.string.toolbox_diff_description, R.string.toolbox_diff_input_label, R.string.toolbox_diff_input_hint, R.string.toolbox_diff_second_label, R.string.toolbox_diff_second_hint),
    ToolboxEntry("base64", R.string.toolbox_base64_name, "data_encoding", R.string.toolbox_base64_description, R.string.toolbox_base64_input_label, R.string.toolbox_base64_input_hint),
    ToolboxEntry("url", R.string.toolbox_url_name, "data_encoding", R.string.toolbox_url_description, R.string.toolbox_url_input_label, R.string.toolbox_url_input_hint),
    ToolboxEntry("json-pointer", R.string.toolbox_json_pointer_name, "data_encoding", R.string.toolbox_json_pointer_description, R.string.toolbox_json_pointer_input_label, R.string.toolbox_json_pointer_input_hint, R.string.toolbox_json_pointer_second_label, R.string.toolbox_json_pointer_second_hint),
    ToolboxEntry("regex", R.string.toolbox_regex_name, "text_regex", R.string.toolbox_regex_description, R.string.toolbox_regex_input_label, R.string.toolbox_regex_input_hint, R.string.toolbox_regex_second_label, R.string.toolbox_regex_second_hint),
    ToolboxEntry("jwt", R.string.toolbox_jwt_name, "developer_utilities", R.string.toolbox_jwt_description, R.string.toolbox_jwt_input_label, R.string.toolbox_jwt_input_hint),
    ToolboxEntry("hash", R.string.toolbox_hash_name, "developer_utilities", R.string.toolbox_hash_description, R.string.toolbox_hash_input_label, R.string.toolbox_hash_input_hint),
    ToolboxEntry("uuid", R.string.toolbox_uuid_name, "developer_utilities", R.string.toolbox_uuid_description, R.string.toolbox_uuid_input_label, R.string.toolbox_uuid_input_hint),
    ToolboxEntry("timestamp", R.string.toolbox_timestamp_name, "developer_utilities", R.string.toolbox_timestamp_description, R.string.toolbox_timestamp_input_label, R.string.toolbox_timestamp_input_hint),
    ToolboxEntry("color", R.string.toolbox_color_name, "android_utilities", R.string.toolbox_color_description, R.string.toolbox_color_input_label, R.string.toolbox_color_input_hint, R.string.toolbox_color_second_label, R.string.toolbox_color_second_hint),
    ToolboxEntry("unicode", R.string.toolbox_unicode_name, "text_regex", R.string.toolbox_unicode_description, R.string.toolbox_unicode_input_label, R.string.toolbox_unicode_input_hint),
    ToolboxEntry("text", R.string.toolbox_text_name, "text_regex", R.string.toolbox_text_description, R.string.toolbox_text_input_label, R.string.toolbox_text_input_hint),
    ToolboxEntry("http-status", R.string.toolbox_http_status_name, "references", R.string.toolbox_http_status_description, R.string.toolbox_http_status_input_label, R.string.toolbox_http_status_input_hint),
    ToolboxEntry("mime", R.string.toolbox_mime_name, "references", R.string.toolbox_mime_description, R.string.toolbox_mime_input_label, R.string.toolbox_mime_input_hint),
    ToolboxEntry("uri", R.string.toolbox_uri_name, "android_utilities", R.string.toolbox_uri_description, R.string.toolbox_uri_input_label, R.string.toolbox_uri_input_hint)
)

private fun modesFor(toolId: String): List<String> = when (toolId) {
    "json" -> listOf("Pretty", "Minify")
    "base64", "url" -> listOf("Encode", "Decode")
    "hash" -> listOf("SHA-256", "SHA-512")
    "text" -> listOf("Default", "UPPERCASE", "lowercase", "Trim", "Sort lines", "Deduplicate lines", "Escape JSON string", "Unescape JSON string")
    else -> emptyList()
}

private fun defaultMode(toolId: String): String = when (toolId) {
    "json" -> "Pretty"
    "base64", "url" -> "Encode"
    "hash" -> "SHA-256"
    else -> "Default"
}

private fun modeResource(mode: String): Int = when (mode) {
    "Pretty" -> R.string.toolbox_mode_pretty
    "Minify" -> R.string.toolbox_mode_minify
    "Encode" -> R.string.toolbox_mode_encode
    "Decode" -> R.string.toolbox_mode_decode
    "SHA-256" -> R.string.toolbox_mode_sha_256
    "SHA-512" -> R.string.toolbox_mode_sha_512
    "UPPERCASE" -> R.string.toolbox_mode_uppercase
    "lowercase" -> R.string.toolbox_mode_lowercase
    "Trim" -> R.string.toolbox_mode_trim
    "Sort lines" -> R.string.toolbox_mode_sort_lines
    "Deduplicate lines" -> R.string.toolbox_mode_deduplicate_lines
    "Escape JSON string" -> R.string.toolbox_mode_escape_json
    "Unescape JSON string" -> R.string.toolbox_mode_unescape_json
    else -> R.string.toolbox_mode_default
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolboxScreen(onBack: () -> Unit, showTopBar: Boolean = true) {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("vibesync_toolbox_v1", 0) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("all") }
    var favorites by remember { mutableStateOf(prefs.getStringSet("favorites", emptySet())?.toSet().orEmpty()) }
    var recent by remember { mutableStateOf(prefs.getString("recent", "").orEmpty().split('|').filter { it.isNotBlank() }.take(8)) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var input by remember { mutableStateOf("") }
    var secondInput by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("Default") }
    var output by remember { mutableStateOf("") }
    var isRunning by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val selectedTool = toolboxEntries.firstOrNull { it.id == selectedId }
    val categories = listOf(
        ToolboxCategory("all", R.string.toolbox_category_all),
        ToolboxCategory("data_encoding", R.string.toolbox_category_data_encoding),
        ToolboxCategory("text_regex", R.string.toolbox_category_text_regex),
        ToolboxCategory("developer_utilities", R.string.toolbox_category_developer_utilities),
        ToolboxCategory("android_utilities", R.string.toolbox_category_android_utilities),
        ToolboxCategory("references", R.string.toolbox_category_references)
    )
    val categoryNames = categories.associate { it.id to stringResource(it.labelRes) }
    val toolNames = toolboxEntries.associate { it.id to stringResource(it.nameRes) }
    val toolDescriptions = toolboxEntries.associate { it.id to stringResource(it.descriptionRes) }
    val resultCopiedMessage = stringResource(R.string.toolbox_result_copied)
    val errorPrefix = stringResource(R.string.toolbox_error_prefix)
    val filtered = toolboxEntries.filter { tool ->
        (category == "all" || tool.categoryId == category) &&
            (toolNames[tool.id].orEmpty().contains(search, ignoreCase = true) ||
             toolDescriptions[tool.id].orEmpty().contains(search, ignoreCase = true) ||
             categoryNames[tool.categoryId].orEmpty().contains(search, ignoreCase = true))
    }.sortedWith(compareByDescending<ToolboxEntry> { it.id in favorites }.thenBy { toolNames[it.id].orEmpty() })

    Scaffold(
        topBar = {
            if (showTopBar) CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.developer_toolbox)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        if (selectedTool != null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { selectedId = null; output = ""; feedback = null }) { Text(stringResource(R.string.back_to_catalogue)) }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val next = if (selectedTool.id in favorites) favorites - selectedTool.id else favorites + selectedTool.id
                        favorites = next
                        prefs.edit().putStringSet("favorites", next).apply()
                    }) { Text(if (selectedTool.id in favorites) stringResource(R.string.favorited) else stringResource(R.string.favorite)) }
                }
                Text(stringResource(selectedTool.nameRes), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(selectedTool.descriptionRes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val modeOptions = modesFor(selectedTool.id)
                if (modeOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.toolbox_operation), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            modeOptions.forEach { option ->
                                FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(stringResource(modeResource(option))) })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; output = ""; feedback = null },
                    label = { Text(stringResource(selectedTool.inputLabelRes)) },
                    placeholder = { Text(stringResource(selectedTool.inputHintRes)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8
                )
                selectedTool.secondLabelRes?.let { labelRes ->
                    OutlinedTextField(
                        value = secondInput,
                        onValueChange = { secondInput = it; output = ""; feedback = null },
                        label = { Text(stringResource(labelRes)) },
                        placeholder = { Text(stringResource(selectedTool.secondHintRes ?: selectedTool.inputHintRes)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        maxLines = 8
                    )
                }
                Button(
                    onClick = {
                        if (isRunning) return@Button
                        isRunning = true
                        output = ""
                        feedback = null
                        scope.launch {
                            val result = withContext(Dispatchers.Default) {
                                runCatching {
                                    DeveloperToolboxTools.execute(
                                        selectedTool.id,
                                        input,
                                        secondInput,
                                        mode
                                    )
                                }
                            }
                            output = result.getOrElse { errorPrefix + (it.message ?: it.javaClass.simpleName) }
                            isRunning = false
                            if (result.isSuccess) {
                                recent = (listOf(selectedTool.id) + recent.filterNot { it == selectedTool.id }).take(8)
                                prefs.edit().putString("recent", recent.joinToString("|")).apply()
                            }
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isRunning) stringResource(R.string.working) else stringResource(R.string.run_offline_tool)) }
                if (output.isNotEmpty()) {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.toolbox_result), style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(output))
                                    feedback = resultCopiedMessage
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Text(stringResource(R.string.copy_response))
                                }
                            }
                            Text(output, style = MaterialTheme.typography.bodyMedium)
                            feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Text(
                    stringResource(R.string.toolbox_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(stringResource(R.string.offline_first_developer_utilities), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
                    Text(stringResource(R.string.toolbox_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text(stringResource(R.string.search_tools)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(3).forEach { group ->
                            FilterChip(selected = category == group.id, onClick = { category = group.id }, label = { Text(stringResource(group.labelRes)) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.drop(3).forEach { group ->
                            FilterChip(selected = category == group.id, onClick = { category = group.id }, label = { Text(stringResource(group.labelRes)) })
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Text(stringResource(R.string.recent_tools), style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            recent.mapNotNull { id -> toolboxEntries.firstOrNull { it.id == id } }.take(4).forEach { tool ->
                                FilterChip(
                                    selected = false,
                                    onClick = {
                                        selectedId = tool.id
                                        input = if (tool.id == "uuid") "1" else ""
                                        secondInput = ""
                                        mode = defaultMode(tool.id)
                                        output = ""
                                        feedback = null
                                    },
                                    label = { Text(stringResource(tool.nameRes).take(18)) }
                                )
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.toolbox_tools_count, filtered.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                items(filtered, key = { it.id }) { tool ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(stringResource(tool.nameRes), style = MaterialTheme.typography.titleMedium)
                                Text(stringResource(categories.first { it.id == tool.categoryId }.labelRes), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(stringResource(tool.descriptionRes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                TextButton(onClick = {
                                    val next = if (tool.id in favorites) favorites - tool.id else favorites + tool.id
                                    favorites = next
                                    prefs.edit().putStringSet("favorites", next).apply()
                                }) { Text(if (tool.id in favorites) "★" else "☆") }
                                TextButton(onClick = {
                                    selectedId = tool.id
                                    input = if (tool.id == "uuid") "1" else ""
                                    secondInput = ""
                                    mode = defaultMode(tool.id)
                                    output = ""
                                    feedback = null
                                }) { Text(stringResource(R.string.open)) }
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) item { Text(stringResource(R.string.no_tools_match), modifier = Modifier.padding(16.dp)) }
                item { Spacer(Modifier.padding(bottom = 12.dp)) }
            }
        }
    }
}
