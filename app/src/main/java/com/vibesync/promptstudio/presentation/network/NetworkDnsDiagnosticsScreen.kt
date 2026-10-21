package com.vibesync.promptstudio.presentation.network

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDnsDiagnosticsScreen(onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    var dnsType by remember { mutableStateOf("A") }
    var urlInput by remember { mutableStateOf("https://example.com") }
    var dnsResult by remember { mutableStateOf("") }
    var headResult by remember { mutableStateOf("") }
    var runningDns by remember { mutableStateOf(false) }
    var runningHead by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val enterHostMessage = stringResource(com.vibesync.promptstudio.R.string.network_enter_host)
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(com.vibesync.promptstudio.R.string.network_diagnostics_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(com.vibesync.promptstudio.R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row {
                            Icon(Icons.Default.Dns, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.size(8.dp))
                            Text(stringResource(com.vibesync.promptstudio.R.string.network_dns_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            stringResource(com.vibesync.promptstudio.R.string.network_dns_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = host,
                            onValueChange = { host = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(com.vibesync.promptstudio.R.string.network_host_label)) },
                            placeholder = { Text("example.com") },
                            singleLine = true
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("A", "AAAA", "MX", "TXT").forEach { type ->
                                FilterChip(
                                    selected = dnsType == type,
                                    onClick = { dnsType = type },
                                    label = { Text(type) }
                                )
                            }
                        }
                        Button(
                            onClick = {
                                val requestedHost = host.trim().trimEnd('.')
                                if (requestedHost.isNotBlank()) {
                                    runningDns = true
                                    dnsResult = ""
                                    scope.launch {
                                        dnsResult = withContext(Dispatchers.IO) {
                                            runCatching { queryDnsOverHttps(requestedHost, dnsType) }
                                                .getOrElse { "Lookup failed: ${it.message ?: it.javaClass.simpleName}" }
                                        }
                                        runningDns = false
                                    }
                                } else {
                                    dnsResult = enterHostMessage
                                }
                            },
                            enabled = !runningDns,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (runningDns) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                            Text(stringResource(com.vibesync.promptstudio.R.string.network_run_dns))
                        }
                        if (dnsResult.isNotBlank()) OutputCard(dnsResult)
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(0.dp).weight(0.1f))
                            Text(stringResource(com.vibesync.promptstudio.R.string.network_http_heading), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            stringResource(com.vibesync.promptstudio.R.string.network_http_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = urlInput,
                            onValueChange = { urlInput = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(com.vibesync.promptstudio.R.string.network_http_url_label)) },
                            placeholder = { Text("https://example.com") },
                            singleLine = true
                        )
                        Button(
                            onClick = {
                                runningHead = true
                                headResult = ""
                                scope.launch {
                                    headResult = withContext(Dispatchers.IO) {
                                        runCatching { requestHead(urlInput.trim()) }
                                            .getOrElse { "Request failed: ${it.message ?: it.javaClass.simpleName}" }
                                    }
                                    runningHead = false
                                }
                            },
                            enabled = !runningHead && urlInput.isNotBlank(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (runningHead) CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp), strokeWidth = 2.dp)
                            Text(stringResource(com.vibesync.promptstudio.R.string.network_run_head))
                        }
                        if (headResult.isNotBlank()) OutputCard(headResult)
                    }
                }
            }
            item {
                Text(
                    stringResource(com.vibesync.promptstudio.R.string.network_privacy_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun OutputCard(output: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        androidx.compose.foundation.text.selection.SelectionContainer {
            Text(
                text = output,
                modifier = Modifier.fillMaxWidth().padding(12.dp),
                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun queryDnsOverHttps(host: String, type: String): String {
    val encodedHost = URLEncoder.encode(host, StandardCharsets.UTF_8.name())
    val connection = URL("https://dns.google/resolve?name=$encodedHost&type=$type")
        .openConnection() as HttpURLConnection
    connection.requestMethod = "GET"
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    connection.setRequestProperty("Accept", "application/dns-json")
    return try {
        val body = connection.inputStream.bufferedReader().use { it.readText() }
        val json = JSONObject(body)
        val answers = json.optJSONArray("Answer")
        val records = buildList {
            if (answers != null) {
                for (index in 0 until answers.length()) {
                    val answer = answers.optJSONObject(index) ?: continue
                    add("${answer.optString("name")}  ${answer.optString("TTL")}s  ${answer.optString("data")}")
                }
            }
        }
        if (records.isEmpty()) {
            "DNS status: ${json.optInt("Status")}\nNo records returned for $type."
        } else {
            "DNS type: $type\nHost: $host\n\n" + records.joinToString("\n")
        }
    } finally {
        connection.disconnect()
    }
}

private fun requestHead(input: String): String {
    val normalized = if (input.startsWith("https://", true)) input else "https://$input"
    val url = URL(normalized)
    require(url.protocol.equals("https", ignoreCase = true)) { "Only HTTPS URLs are accepted." }
    val connection = url.openConnection() as HttpURLConnection
    connection.requestMethod = "HEAD"
    connection.connectTimeout = 8000
    connection.readTimeout = 8000
    connection.instanceFollowRedirects = false
    return try {
        val code = connection.responseCode
        val headers = connection.headerFields.entries
            .filter { !it.key.isNullOrBlank() }
            .sortedBy { it.key!!.lowercase(java.util.Locale.ROOT) }
            .joinToString("\n") { (name, values) -> "$name: ${values.orEmpty().joinToString(", ")}" }
        "HTTP status: $code\nURL: ${connection.url}\n\n" + headers.ifBlank { "No response headers were provided." }
    } finally {
        connection.disconnect()
    }
}
