package com.vibesync.promptstudio.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibesync.promptstudio.domain.model.ProviderType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onConnectOAuth: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var providerMenuExpanded by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text("Secure Settings") },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypted credential vault", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(state.vaultStatus.message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Secrets are encrypted with AES-GCM before storage. Android Keystore backing depends on the device; credentials are not logged or sent to a proxy.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Provider API key", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { providerMenuExpanded = true }) {
                        Text(state.selectedProvider.displayName + if (state.selectedProvider in state.savedProviders) " • key saved" else " • no key saved")
                    }
                    DropdownMenu(expanded = providerMenuExpanded, onDismissRequest = { providerMenuExpanded = false }) {
                        ProviderType.entries.forEach { provider ->
                            DropdownMenuItem(text = { Text(provider.displayName) }, onClick = {
                                viewModel.selectProvider(provider)
                                providerMenuExpanded = false
                            })
                        }
                    }
                    OutlinedTextField(
                        value = state.secretInput,
                        onValueChange = viewModel::updateSecret,
                        label = { Text("API key") },
                        placeholder = { Text("Paste key; saved value is never shown again") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::saveKey, enabled = !state.busy && state.secretInput.isNotBlank()) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Save key")
                        }
                        TextButton(onClick = viewModel::deleteKey, enabled = state.selectedProvider in state.savedProviders) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text("Remove")
                        }
                    }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("OAuth 2.0 / PKCE", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "OAuth is disabled until an authorized public-client ID, HTTPS authorization endpoint, and HTTPS token endpoint are supplied at build time. The flow validates state and PKCE, but its identity token is stored separately and is not used as an API key.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConnectOAuth, enabled = state.oauthConfigured, modifier = Modifier.fillMaxWidth()) {
                        Text("Connect account with browser")
                    }
                    if (!state.oauthConfigured) {
                        Text(
                            "Set VIBESYNC_OAUTH_CLIENT_ID, VIBESYNC_OAUTH_AUTHORIZE_ENDPOINT, and VIBESYNC_OAUTH_TOKEN_ENDPOINT only to values issued for an OAuth client authorized by that provider.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                "Provider requests are sent directly from this device over HTTPS. Keep API keys private and rotate any key you suspect is exposed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
