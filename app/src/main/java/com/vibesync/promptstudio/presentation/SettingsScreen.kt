package com.vibesync.promptstudio.presentation
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.data.ads.AdMobManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onConnectOAuth: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    adMobManager: AdMobManager,
    onManageAdsPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyOptionsRequired by adMobManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var showVaultResetConfirmation by remember { mutableStateOf(false) }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(stringResource(R.string.secure_settings)) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back)) } }
        )
    }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_encrypted_credential_vault), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(state.vaultStatus.message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Secrets use AES-GCM with a non-exportable Android Keystore key. A metadata-check failure does not disable working encryption. No plaintext or weaker-cipher fallback is used.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!state.vaultStatus.initialized) {
                        Text(
                            "Secure credential saving is disabled until the Keystore status is healthy. Existing ciphertext is not silently deleted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.vaultStatus.recoveryRequired) {
                        Text(
                            "The original key is missing or invalidated. Existing encrypted credentials cannot be recovered from the ciphertext.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                        OutlinedButton(onClick = { showVaultResetConfirmation = true }, enabled = !state.busy) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.reset_lost_key_vault))
                        }
                    }
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.provider_api_key), style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { providerMenuExpanded = true }) {
                        Text(if (state.selectedProvider in state.savedProviders) stringResource(R.string.provider_key_saved, state.selectedProvider.displayName) else stringResource(R.string.provider_no_key_saved, state.selectedProvider.displayName))
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
                        label = { Text(stringResource(R.string.api_key)) },
                        placeholder = { Text(stringResource(R.string.paste_api_key_hint)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = viewModel::saveKey, enabled = state.vaultStatus.initialized && !state.busy && state.secretInput.isNotBlank()) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.save_key))
                        }
                        TextButton(onClick = viewModel::deleteKey, enabled = state.selectedProvider in state.savedProviders) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(4.dp))
                            Text(stringResource(R.string.remove))
                        }
                    }
                    state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
                }
            }
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.oauth_pkce), style = MaterialTheme.typography.titleMedium)
                    Text(
                        "This is generic OAuth Authorization Code + PKCE plumbing, not a provider sign-in integration. No OpenAI, Anthropic, or Gemini OAuth support is implied by filling in endpoints. Only use a documented provider-supported public-client flow; identity tokens are never used as model API keys.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(onClick = onConnectOAuth, enabled = state.oauthConfigured, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.connect_account_browser))
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
            Card {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.BugReport, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.developer_tools), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        "Inspect app-collected logs, sanitized provider HTTP metadata, historical process-exit details, network state, and export a diagnostic report. Full system Logcat remains restricted by Android.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(onClick = onOpenDiagnostics, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.open_developer_diagnostics)) }
                    if (privacyOptionsRequired) {
                        TextButton(onClick = onManageAdsPrivacy, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.manage_ad_privacy))
                        }
                    }
                    TextButton(onClick = {
                        val listing = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + context.packageName))
                        runCatching { ContextCompat.startActivity(context, listing, null) }
                    }, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.open_play_listing))
                    }
                }
            }
            Text(
                "Provider requests are sent directly from this device over HTTPS. Keep API keys private and rotate any key you suspect is exposed.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showVaultResetConfirmation) {
        AlertDialog(
            onDismissRequest = { showVaultResetConfirmation = false },
            title = { Text(stringResource(R.string.reset_vault_title)) },
            text = {
                Text(
                    "The original Android Keystore key is missing or invalidated. Credentials encrypted with that key cannot be recovered. Continuing deletes the remaining encrypted vault entries and key alias from this app, so you must re-enter every API key. This does not affect saved prompt templates or run history."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showVaultResetConfirmation = false
                    viewModel.resetVaultAfterKeyLoss()
                }) { Text(stringResource(R.string.reset_vault)) }
            },
            dismissButton = {
                TextButton(onClick = { showVaultResetConfirmation = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
