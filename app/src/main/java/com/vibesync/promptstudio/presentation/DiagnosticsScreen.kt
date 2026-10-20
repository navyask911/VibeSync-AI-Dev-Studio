package com.vibesync.promptstudio.presentation
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.vibesync.promptstudio.data.diagnostics.DiagnosticRecord
import com.vibesync.promptstudio.data.diagnostics.DiagnosticSeverity
import com.vibesync.promptstudio.data.diagnostics.DiagnosticsStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticsScreen(store: DiagnosticsStore, onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var keyword by remember { mutableStateOf("") }
    var component by remember { mutableStateOf("") }
    var provider by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var processIdText by remember { mutableStateOf("") }
    var severityName by remember { mutableStateOf("ALL") }
    var timeRange by remember { mutableStateOf("ALL") }
    var refreshTick by remember { mutableIntStateOf(0) }
    var showClearConfirmation by remember { mutableStateOf(false) }
    var feedback by remember { mutableStateOf<String?>(null) }

    val severity = remember(severityName) {
        if (severityName == "ALL") null else runCatching { DiagnosticSeverity.valueOf(severityName) }.getOrNull()
    }
    val fromTimestamp = remember(timeRange, refreshTick) {
        when (timeRange) {
            "1 hour" -> System.currentTimeMillis() - 60L * 60L * 1000L
            "24 hours" -> System.currentTimeMillis() - 24L * 60L * 60L * 1000L
            "7 days" -> System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
            else -> null
        }
    }
    val records = remember(keyword, component, provider, category, processIdText, severity, fromTimestamp, refreshTick) {
        store.logs(
            severity = severity,
            query = keyword,
            component = component,
            provider = provider,
            category = category,
            processId = processIdText.trim().toIntOrNull(),
            fromTimestampMillis = fromTimestamp
        )
    }
    val report = remember(records, refreshTick) { store.report(records) }

    LaunchedEffect(Unit) {
        store.record(
            DiagnosticSeverity.INFO,
            "diagnostics",
            "navigation",
            "screen_opened",
            "Developer Diagnostics screen opened."
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.developer_diagnostics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { refreshTick++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh_diagnostics))
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
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.diagnostics_app_device_details), style = MaterialTheme.typography.titleMedium)
                        store.environmentSummary().take(8).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Text(
                            stringResource(R.string.diagnostics_app_logcat_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.diagnostics_search_and_filter), style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = keyword,
                            onValueChange = { keyword = it },
                            label = { Text(stringResource(R.string.keyword_or_error_text)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = component,
                            onValueChange = { component = it },
                            label = { Text(stringResource(R.string.component_tag_optional)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = provider,
                            onValueChange = { provider = it },
                            label = { Text(stringResource(R.string.provider_examples)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text(stringResource(R.string.category_operation)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = processIdText,
                            onValueChange = { processIdText = it.filter(Char::isDigit).take(10) },
                            label = { Text(stringResource(R.string.process_id_optional)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "VERBOSE", "DEBUG").forEach { name ->
                                FilterChip(
                                    selected = severityName == name,
                                    onClick = { severityName = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("INFO", "WARNING", "ERROR").forEach { name ->
                                FilterChip(
                                    selected = severityName == name,
                                    onClick = { severityName = name },
                                    label = { Text(name) }
                                )
                            }
                        }
                        Text(stringResource(R.string.diagnostics_time_range), style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("ALL", "1 hour", "24 hours", "7 days").forEach { range ->
                                FilterChip(
                                    selected = timeRange == range,
                                    onClick = { timeRange = range; refreshTick++ },
                                    label = { Text(range) }
                                )
                            }
                        }
                        Text(stringResource(R.string.diagnostic_matching_records, records.size), style = MaterialTheme.typography.bodySmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = {
                                clipboard.setText(AnnotatedString(report))
                                feedback = "Sanitized diagnostic report copied."
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null)
                                Text(stringResource(R.string.copy_report))
                            }
                            Button(onClick = {
                                runCatching {
                                    val file = store.exportReport(records)
                                    val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, "Sanitized VibeSync diagnostic report")
                                        clipData = ClipData.newUri(context.contentResolver, "Diagnostic report", uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    ContextCompat.startActivity(context, Intent.createChooser(send, "Share diagnostic report"), null)
                                }.onSuccess { feedback = "Diagnostic export prepared." }
                                    .onFailure { feedback = "Could not export diagnostics: " + (it.message ?: "unknown error") }
                            }) {
                                Icon(Icons.Default.IosShare, contentDescription = null)
                                Text(stringResource(R.string.share_export))
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = { showClearConfirmation = true }) {
                                Icon(Icons.Default.Delete, contentDescription = null)
                                Text(stringResource(R.string.clear_local_logs))
                            }
                            TextButton(onClick = {
                                val adbText = "For full device/app Logcat, connect a development computer and run:\nadb logcat -d -t 500\n\nFor bug reports, use Android Studio or Developer options > Take bug report. App-only records can be exported above."
                                clipboard.setText(AnnotatedString(adbText))
                                feedback = "ADB collection instructions copied."
                            }) { Text(stringResource(R.string.copy_adb_instructions)) }
                        }
                        feedback?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                    }
                }
            }
            item {
                Card {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(R.string.historical_process_exits), style = MaterialTheme.typography.titleMedium)
                        val exits = store.processExitSummaries()
                        if (exits.isEmpty()) {
                            Text(stringResource(R.string.no_historical_process_exits), style = MaterialTheme.typography.bodySmall)
                        } else {
                            exits.forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }
                }
            }
            if (records.isEmpty()) {
                item {
                    Card {
                        Text(
                            stringResource(R.string.diagnostics_no_matching_records),
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            } else {
                itemsIndexed(records, key = { index, record -> record.timestampMillis.toString() + record.operation + index }) { _, record ->
                    DiagnosticRecordCard(record)
                }
            }
        }
    }

    if (showClearConfirmation) {
        AlertDialog(
            onDismissRequest = { showClearConfirmation = false },
            title = { Text(stringResource(R.string.clear_diagnostics_title)) },
            text = { Text(stringResource(R.string.clear_diagnostics_message)) },
            confirmButton = {
                TextButton(onClick = {
                    store.clear()
                    showClearConfirmation = false
                    refreshTick++
                    feedback = "Local diagnostic records cleared."
                }) { Text(stringResource(R.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun DiagnosticRecordCard(record: DiagnosticRecord) {
    Card {
        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                record.severity.name + " • " + record.component + "/" + record.category,
                color = when (record.severity) {
                    DiagnosticSeverity.ERROR -> MaterialTheme.colorScheme.error
                    DiagnosticSeverity.WARNING -> MaterialTheme.colorScheme.tertiary
                    else -> MaterialTheme.colorScheme.primary
                },
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(record.timestampMillis)) +
                    " • " + record.operation +
                    (record.provider?.let { " • " + it } ?: "") +
                    " • pid " + record.processId,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(record.message, style = MaterialTheme.typography.bodySmall)
        }
    }
}
