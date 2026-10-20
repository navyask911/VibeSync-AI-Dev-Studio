package com.vibesync.promptstudio.presentation

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
    val name: String,
    val category: String,
    val description: String,
    val inputLabel: String = "Input",
    val inputHint: String = "Enter text",
    val secondLabel: String? = null,
    val secondHint: String = ""
)

private val toolboxEntries = listOf(
    ToolboxEntry("json", "JSON Formatter & Validator", "Data & Encoding", "Pretty-print or minify JSON with parse validation.", inputLabel = "JSON", inputHint = "{\"name\":\"Ada\",\"active\":true}"),
    ToolboxEntry("xml", "XML Formatter & Validator", "Data & Encoding", "Formats well-formed XML with external entities disabled.", inputLabel = "XML", inputHint = "<root><item>value</item></root>"),
    ToolboxEntry("diff", "Text Diff", "Text & Regex", "Compare two text blocks with line-by-line changes.", inputLabel = "Original text", inputHint = "First version", secondLabel = "Updated text", secondHint = "Second version"),
    ToolboxEntry("base64", "Base64 Encoder / Decoder", "Data & Encoding", "Encode UTF-8 text or decode standard and URL-safe Base64.", inputLabel = "Text or Base64", inputHint = "Hello VibeSync"),
    ToolboxEntry("url", "URL Component Encoder", "Data & Encoding", "Encode or decode a URI component. Secret query keys are redacted in URI preview.", inputLabel = "URL component", inputHint = "hello world & query"),
    ToolboxEntry("json-pointer", "JSON Pointer Explorer", "Data & Encoding", "Inspect a nested JSON value using RFC 6901-style JSON Pointer.", inputLabel = "JSON document", inputHint = "{\"user\":{\"name\":\"Ada\"}}", secondLabel = "JSON Pointer", secondHint = "/user/name"),
    ToolboxEntry("regex", "Safe Regex Tester", "Text & Regex", "Test common patterns using RE2/J's linear-time engine (look-around/backreferences unsupported).", inputLabel = "Regular expression", inputHint = "(cat|dog)+", secondLabel = "Test text", secondHint = "cat dog cat"),
    ToolboxEntry("jwt", "JWT Inspector", "Developer Utilities", "Decode compact JWT header and payload locally; does not validate signature or trust.", inputLabel = "JWT", inputHint = "header.payload.signature"),
    ToolboxEntry("hash", "Hash / Checksum Calculator", "Developer Utilities", "Calculate SHA-256 or SHA-512. A hash is not encryption.", inputLabel = "Text to hash", inputHint = "A message"),
    ToolboxEntry("uuid", "UUID Generator", "Developer Utilities", "Generate one to 100 random UUID identifiers.", inputLabel = "Number of UUIDs", inputHint = "1"),
    ToolboxEntry("timestamp", "Timestamp Converter", "Developer Utilities", "Convert epoch seconds/milliseconds and ISO-8601 UTC timestamps.", inputLabel = "Timestamp", inputHint = "1767225600 or 2026-01-01T00:00:00Z"),
    ToolboxEntry("color", "Color Contrast Checker", "Android Utilities", "Calculate WCAG contrast ratio for two HEX colors.", inputLabel = "Foreground HEX", inputHint = "#111111", secondLabel = "Background HEX", secondHint = "#FFFFFF"),
    ToolboxEntry("unicode", "Unicode / Text Inspector", "Text & Regex", "Inspect code points, UTF-16 units, UTF-8 bytes, and line endings.", inputLabel = "Text", inputHint = "Paste text or emoji"),
    ToolboxEntry("text", "Text Transformation Toolbox", "Text & Regex", "Change case, trim, sort/deduplicate lines, or escape JSON strings.", inputLabel = "Text", inputHint = "Enter text"),
    ToolboxEntry("http-status", "HTTP Status Reference", "References", "Search common HTTP status codes and troubleshooting hints.", inputLabel = "Status code or keyword", inputHint = "429 or timeout"),
    ToolboxEntry("mime", "MIME Type Reference", "References", "Look up a compact built-in extension/MIME type reference.", inputLabel = "Extension or MIME type", inputHint = ".json or application/json"),
    ToolboxEntry("uri", "URI / Deep Link Inspector", "Android Utilities", "Preview URI parts and redact common secret query keys. Does not launch the URI.", inputLabel = "URI", inputHint = "https://example.com/path?token=secret")
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperToolboxScreen(onBack: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val prefs = remember(context) { context.getSharedPreferences("vibesync_toolbox_v1", 0) }
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()

    var search by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
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
    val categories = listOf("All", "Data & Encoding", "Text & Regex", "Developer Utilities", "Android Utilities", "References")
    val filtered = toolboxEntries.filter { tool ->
        (category == "All" || tool.category == category) &&
            (tool.name.contains(search, ignoreCase = true) ||
             tool.description.contains(search, ignoreCase = true) ||
             tool.category.contains(search, ignoreCase = true))
    }.sortedWith(compareByDescending<ToolboxEntry> { it.id in favorites }.thenBy { it.name })

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Developer Toolbox") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                    TextButton(onClick = { selectedId = null; output = ""; feedback = null }) { Text("← Catalogue") }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = {
                        val next = if (selectedTool.id in favorites) favorites - selectedTool.id else favorites + selectedTool.id
                        favorites = next
                        prefs.edit().putStringSet("favorites", next).apply()
                    }) { Text(if (selectedTool.id in favorites) "★ Favorited" else "☆ Favorite") }
                }
                Text(selectedTool.name, style = MaterialTheme.typography.headlineSmall)
                Text(selectedTool.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val modeOptions = modesFor(selectedTool.id)
                if (modeOptions.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Operation", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            modeOptions.forEach { option ->
                                FilterChip(selected = mode == option, onClick = { mode = option }, label = { Text(option) })
                            }
                        }
                    }
                }
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it; output = ""; feedback = null },
                    label = { Text(selectedTool.inputLabel) },
                    placeholder = { Text(selectedTool.inputHint) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 8
                )
                selectedTool.secondLabel?.let { label ->
                    OutlinedTextField(
                        value = secondInput,
                        onValueChange = { secondInput = it; output = ""; feedback = null },
                        label = { Text(label) },
                        placeholder = { Text(selectedTool.secondHint) },
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
                            output = result.getOrElse { "Error: " + (it.message ?: it.javaClass.simpleName) }
                            isRunning = false
                            if (result.isSuccess) {
                                recent = (listOf(selectedTool.id) + recent.filterNot { it == selectedTool.id }).take(8)
                                prefs.edit().putString("recent", recent.joinToString("|")).apply()
                            }
                        }
                    },
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth()
                ) { Text(if (isRunning) "Working…" else "Run offline tool") }
                if (output.isNotEmpty()) {
                    Card {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Result", style = MaterialTheme.typography.titleMedium)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = {
                                    clipboard.setText(AnnotatedString(output))
                                    feedback = "Result copied."
                                }) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    Text("Copy")
                                }
                            }
                            Text(output, style = MaterialTheme.typography.bodyMedium)
                            feedback?.let { Text(it, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
                Text(
                    "Tools run locally unless a future tool explicitly identifies a network action. JWT decoding is not validation; HTTP/MIME reference data is compact and may not be exhaustive.",
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
                    Text("Offline-first developer utilities", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 10.dp))
                    Text("Search, favorite, and reuse tools. No root or special permissions required.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = search, onValueChange = { search = it }, label = { Text("Search tools") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.take(3).forEach { group ->
                            FilterChip(selected = category == group, onClick = { category = group }, label = { Text(group) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        categories.drop(3).forEach { group ->
                            FilterChip(selected = category == group, onClick = { category = group }, label = { Text(group) })
                        }
                    }
                    if (recent.isNotEmpty()) {
                        Text("Recent tools", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
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
                                    label = { Text(tool.name.take(18)) }
                                )
                            }
                        }
                    }
                    Text(
                        (filtered.size).toString() + " tools",
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
                                Text(tool.name, style = MaterialTheme.typography.titleMedium)
                                Text(tool.category, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                Text(tool.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                }) { Text("Open") }
                            }
                        }
                    }
                }
                if (filtered.isEmpty()) item { Text("No tools match this search.", modifier = Modifier.padding(16.dp)) }
                item { Spacer(Modifier.padding(bottom = 12.dp)) }
            }
        }
    }
}
