package com.vibesync.promptstudio.presentation.diagnostics

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

private enum class LogLevel(val code: String, val rank: Int) {
    VERBOSE("V", 2), DEBUG("D", 3), INFO("I", 4), WARN("W", 5), ERROR("E", 6)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveLogcatScreen(
    onBack: () -> Unit,
    onOpenDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var capturing by remember { mutableStateOf(false) }
    var filterText by remember { mutableStateOf("") }
    var minimumLevel by remember { mutableStateOf(LogLevel.DEBUG) }
    var captureError by remember { mutableStateOf("") }
    var runningProcess by remember { mutableStateOf<java.lang.Process?>(null) }
    val lines = remember { mutableStateListOf<String>() }
    val activeFilter by rememberUpdatedState(filterText)
    val activeMinimumLevel by rememberUpdatedState(minimumLevel)

    BackHandler(onBack = {
        runningProcess?.destroy()
        capturing = false
        onBack()
    })
    DisposableEffect(Unit) {
        onDispose {
            runCatching { runningProcess?.destroy() }
            runningProcess = null
        }
    }

    LaunchedEffect(capturing) {
        if (!capturing) return@LaunchedEffect
        var process: java.lang.Process? = null
        try {
            process = withContext(Dispatchers.IO) {
                ProcessBuilder(
                    "/system/bin/logcat", "-v", "time",
                    "--pid=${android.os.Process.myPid()}"
                ).redirectErrorStream(true).start()
            }
            runningProcess = process
            val reader = process.inputStream.bufferedReader()
            while (isActive) {
                val line = withContext(Dispatchers.IO) { reader.readLine() } ?: break
                val lineLevel = parseLevel(line)
                val levelMatches = lineLevel == null || lineLevel.rank >= activeMinimumLevel.rank
                val textMatches = activeFilter.isBlank() || line.contains(activeFilter, ignoreCase = true)
                if (levelMatches && textMatches) {
                    lines.add(sanitizeLogLine(line))
                    while (lines.size > 350) lines.removeAt(0)
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            captureError = error.message ?: error.javaClass.simpleName
            capturing = false
        } finally {
            withContext(Dispatchers.IO) {
                runCatching { process?.destroy() }
                runCatching { process?.inputStream?.close() }
            }
            if (runningProcess === process) runningProcess = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.logcat_live_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = {
                        runningProcess?.destroy()
                        capturing = false
                        onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.logcat_scope, context.packageName), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            stringResource(R.string.logcat_limit_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = filterText,
                            onValueChange = { filterText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.logcat_filter_label)) },
                            placeholder = { Text(stringResource(R.string.logcat_filter_hint)) },
                            singleLine = true
                        )
                        Text(stringResource(R.string.logcat_level), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            LogLevel.entries.forEach { level ->
                                FilterChip(
                                    selected = minimumLevel == level,
                                    onClick = { minimumLevel = level },
                                    label = { Text(level.code) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(
                                onClick = {
                                    captureError = ""
                                    if (capturing) {
                                        runningProcess?.destroy()
                                        capturing = false
                                    } else {
                                        capturing = true
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(if (capturing) R.string.logcat_stop else R.string.logcat_start))
                            }
                            OutlinedButton(
                                onClick = { lines.clear(); captureError = "" },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.logcat_clear)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(lines.joinToString("\n")))
                                    Toast.makeText(context, context.getString(R.string.copy_response), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.weight(1f),
                                enabled = lines.isNotEmpty()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Spacer(Modifier.height(2.dp))
                                Text(stringResource(R.string.logcat_copy))
                            }
                            OutlinedButton(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_SEND)
                                        .setType("text/plain")
                                        .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.logcat_live_title))
                                        .putExtra(Intent.EXTRA_TEXT, lines.joinToString("\n"))
                                    runCatching { context.startActivity(Intent.createChooser(intent, context.getString(R.string.logcat_share))) }
                                        .onFailure { Toast.makeText(context, it.message ?: context.getString(R.string.devoptions_open_failed), Toast.LENGTH_LONG).show() }
                                },
                                modifier = Modifier.weight(1f),
                                enabled = lines.isNotEmpty()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null)
                                Text(stringResource(R.string.logcat_share))
                            }
                        }
                        if (captureError.isNotBlank()) {
                            Text(
                                stringResource(R.string.logcat_capture_error, captureError),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    if (lines.isEmpty()) {
                        Text(
                            stringResource(R.string.logcat_no_lines),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        SelectionContainer {
                            Text(
                                text = lines.joinToString("\n").takeLast(22000),
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                            )
                        }
                    }
                }
            }
            item {
                OutlinedButton(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.logcat_open_diagnostics))
                }
            }
        }
    }
}

private fun parseLevel(line: String): LogLevel? {
    // Handles both compact "D/Tag:" and "-v time" output "... 1234 1234 D Tag:".
    val code = Regex("(?:^|\\s)([VDIWEF])(?:\\s+[^\\s:]+:|/)")
        .find(line)?.groupValues?.getOrNull(1) ?: return null
    return LogLevel.entries.firstOrNull { it.code == code } ?: if (code == "F") LogLevel.ERROR else null
}

private fun sanitizeLogLine(line: String): String {
    val authorization = Regex("(?i)(authorization\\s*[:=]\\s*)(?:bearer\\s+|basic\\s+)?[^\\s,;]+")
    val authorizationRedacted = authorization.replace(line) { match ->
        "${match.groupValues[1]}[REDACTED]"
    }
    val bearerTokens = Regex("(?i)(\\bbearer\\s+)[^\\s,;]+")
    val bearerRedacted = bearerTokens.replace(authorizationRedacted) { match ->
        "${match.groupValues[1]}[REDACTED]"
    }
    val secrets = Regex(
        "(?i)(api[_-]?key|access[_-]?token|refresh[_-]?token|password|secret)(\\s*[=:]\\s*|\\s+)[^\\s,;]+"
    )
    return secrets.replace(bearerRedacted) { match -> "${match.groupValues[1]}=[REDACTED]" }
}
