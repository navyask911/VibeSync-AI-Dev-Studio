package com.vibesync.promptstudio.presentation
import androidx.compose.ui.res.stringResource
import com.vibesync.promptstudio.R

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vibesync.promptstudio.domain.model.ProviderType
import com.vibesync.promptstudio.data.ads.AdMobManager
import com.vibesync.promptstudio.presentation.auth.OAuthClientConfiguration

private enum class OAuthProviderPreset(
    val titleRes: Int,
    val authorizeEndpoint: String,
    val tokenEndpoint: String,
    val scopes: String,
    val supportsPublicOAuth: Boolean,
    val editableEndpoints: Boolean
) {
    GOOGLE_AI_STUDIO(R.string.provider_google_ai_studio, "https://accounts.google.com/o/oauth2/v2/auth", "https://oauth2.googleapis.com/token", "openid email profile", true, false),
    OPENAI_API(R.string.provider_openai_api, "", "", "", false, false),
    ANTHROPIC_API(R.string.provider_anthropic_api, "", "", "", false, false),
    CUSTOM_ENDPOINT(R.string.provider_custom_endpoint, "", "", "", true, true)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onConnectOAuth: (OAuthClientConfiguration) -> Unit,
    onOpenDiagnostics: () -> Unit,
    adMobManager: AdMobManager,
    onManageAdsPrivacy: () -> Unit,
    showTopBar: Boolean = true
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val privacyOptionsRequired by adMobManager.privacyOptionsRequired.collectAsStateWithLifecycle()
    val appearancePrefs = remember(context) { context.getSharedPreferences("vibesync_appearance", 0) }
    var themeMode by remember(appearancePrefs) { mutableStateOf(appearancePrefs.getString("theme_mode", "normal") ?: "normal") }
    var fontChoice by remember(appearancePrefs) { mutableStateOf(appearancePrefs.getString("font_family", "serif") ?: "serif") }
    var providerMenuExpanded by remember { mutableStateOf(false) }
    var oauthProviderMenuExpanded by remember { mutableStateOf(false) }
    var showVaultResetConfirmation by remember { mutableStateOf(false) }
    val oauthPrefs = remember(context) { context.getSharedPreferences("vibesync_oauth_settings", 0) }
    val initialOAuthProvider = remember(oauthPrefs) {
        runCatching { OAuthProviderPreset.valueOf(oauthPrefs.getString("provider", "GOOGLE_AI_STUDIO").orEmpty()) }
            .getOrDefault(OAuthProviderPreset.GOOGLE_AI_STUDIO)
    }
    var oauthProvider by remember { mutableStateOf(initialOAuthProvider) }
    var oauthClientId by remember(oauthProvider) { mutableStateOf(oauthPrefs.getString(oauthProvider.name + ".client_id", "").orEmpty()) }
    var oauthAuthorizeEndpoint by remember(oauthProvider) { mutableStateOf(oauthPrefs.getString(oauthProvider.name + ".authorize_endpoint", oauthProvider.authorizeEndpoint).orEmpty()) }
    var oauthTokenEndpoint by remember(oauthProvider) { mutableStateOf(oauthPrefs.getString(oauthProvider.name + ".token_endpoint", oauthProvider.tokenEndpoint).orEmpty()) }
    var oauthScopes by remember(oauthProvider) { mutableStateOf(oauthPrefs.getString(oauthProvider.name + ".scopes", oauthProvider.scopes).orEmpty()) }
    var oauthRedirectUri by remember(oauthProvider) { mutableStateOf(oauthPrefs.getString(oauthProvider.name + ".redirect_uri", OAuthClientConfiguration.DEFAULT_REDIRECT_URI).orEmpty()) }
    val oauthReady = oauthProvider.supportsPublicOAuth && oauthClientId.isNotBlank() &&
        isHttpsEndpoint(oauthAuthorizeEndpoint) && isHttpsEndpoint(oauthTokenEndpoint) &&
        oauthScopes.isNotBlank() && oauthRedirectUri == OAuthClientConfiguration.DEFAULT_REDIRECT_URI

    Scaffold(topBar = {
        if (showTopBar) CenterAlignedTopAppBar(
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
                    Text(stringResource(R.string.appearance_settings), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.appearance_description), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.appearance_theme_label), style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = themeMode == "normal", onClick = { themeMode = "normal"; appearancePrefs.edit().putString("theme_mode", themeMode).apply() }, label = { Text(stringResource(R.string.theme_normal)) })
                        FilterChip(selected = themeMode == "developer_dark", onClick = { themeMode = "developer_dark"; appearancePrefs.edit().putString("theme_mode", themeMode).apply() }, label = { Text(stringResource(R.string.theme_developer_dark)) })
                        FilterChip(selected = themeMode == "system", onClick = { themeMode = "system"; appearancePrefs.edit().putString("theme_mode", themeMode).apply() }, label = { Text(stringResource(R.string.theme_system)) })
                    }
                    Text(stringResource(R.string.appearance_font_label), style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = fontChoice == "serif", onClick = { fontChoice = "serif"; appearancePrefs.edit().putString("font_family", fontChoice).apply() }, label = { Text(stringResource(R.string.font_serif_tnr)) })
                        FilterChip(selected = fontChoice == "sans", onClick = { fontChoice = "sans"; appearancePrefs.edit().putString("font_family", fontChoice).apply() }, label = { Text(stringResource(R.string.font_sans)) })
                        FilterChip(selected = fontChoice == "mono", onClick = { fontChoice = "mono"; appearancePrefs.edit().putString("font_family", fontChoice).apply() }, label = { Text(stringResource(R.string.font_mono)) })
                    }
                    Text(stringResource(R.string.font_license_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Text(stringResource(R.string.appearance_preview_title), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(stringResource(R.string.appearance_preview_heading), style = MaterialTheme.typography.titleLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                            Text(stringResource(R.string.appearance_preview_body), style = MaterialTheme.typography.bodyMedium)
                            Text(stringResource(R.string.appearance_preview_code), style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace))
                        }
                    }
                }
            }
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_encrypted_credential_vault), style = MaterialTheme.typography.titleMedium)
                    }
                    Text(state.vaultStatus.message, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.settings_keystore_security_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!state.vaultStatus.initialized) {
                        Text(
                            stringResource(R.string.settings_keystore_save_disabled_note),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    if (state.vaultStatus.recoveryRequired) {
                        Text(
                            stringResource(R.string.settings_keystore_recovery_note),
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
                    Text(stringResource(R.string.oauth_login_browser_notice), style = MaterialTheme.typography.bodyMedium)
                    Box {
                        OutlinedButton(onClick = { oauthProviderMenuExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(oauthProvider.titleRes), modifier = Modifier.weight(1f))
                        }
                        DropdownMenu(expanded = oauthProviderMenuExpanded, onDismissRequest = { oauthProviderMenuExpanded = false }) {
                            OAuthProviderPreset.entries.forEach { option ->
                                DropdownMenuItem(text = { Text(stringResource(option.titleRes)) }, onClick = {
                                    oauthProvider = option
                                    oauthPrefs.edit().putString("provider", option.name).apply()
                                    oauthProviderMenuExpanded = false
                                })
                            }
                        }
                    }
                    OutlinedTextField(value = oauthClientId, onValueChange = { oauthClientId = it; oauthPrefs.edit().putString(oauthProvider.name + ".client_id", it).apply() }, label = { Text(stringResource(R.string.oauth_client_id)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = oauthProvider.supportsPublicOAuth)
                    OutlinedTextField(value = oauthAuthorizeEndpoint, onValueChange = { oauthAuthorizeEndpoint = it; oauthPrefs.edit().putString(oauthProvider.name + ".authorize_endpoint", it).apply() }, label = { Text(stringResource(R.string.oauth_authorize_endpoint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = oauthProvider.editableEndpoints)
                    OutlinedTextField(value = oauthTokenEndpoint, onValueChange = { oauthTokenEndpoint = it; oauthPrefs.edit().putString(oauthProvider.name + ".token_endpoint", it).apply() }, label = { Text(stringResource(R.string.oauth_token_endpoint)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = oauthProvider.editableEndpoints)
                    OutlinedTextField(value = oauthScopes, onValueChange = { oauthScopes = it; oauthPrefs.edit().putString(oauthProvider.name + ".scopes", it).apply() }, label = { Text(stringResource(R.string.oauth_scopes)) }, modifier = Modifier.fillMaxWidth(), enabled = oauthProvider.supportsPublicOAuth)
                    OutlinedTextField(value = oauthRedirectUri, onValueChange = { oauthRedirectUri = it; oauthPrefs.edit().putString(oauthProvider.name + ".redirect_uri", it).apply() }, label = { Text(stringResource(R.string.oauth_redirect_uri)) }, modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = oauthProvider.editableEndpoints)
                    when (oauthProvider) {
                        OAuthProviderPreset.GOOGLE_AI_STUDIO -> Text(stringResource(R.string.oauth_google_identity_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OAuthProviderPreset.OPENAI_API, OAuthProviderPreset.ANTHROPIC_API -> Text(stringResource(R.string.oauth_api_key_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OAuthProviderPreset.CUSTOM_ENDPOINT -> Text(stringResource(R.string.oauth_custom_endpoint_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(onClick = {
                        onConnectOAuth(OAuthClientConfiguration(
                            clientId = oauthClientId.trim(),
                            authorizeEndpoint = oauthAuthorizeEndpoint.trim(),
                            tokenEndpoint = oauthTokenEndpoint.trim(),
                            redirectUri = oauthRedirectUri.trim(),
                            scopes = oauthScopes.trim()
                        ))
                    }, enabled = oauthReady, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.connect_account_browser))
                    }
                    if (!oauthProvider.supportsPublicOAuth) {
                        Text(stringResource(R.string.oauth_connect_unavailable), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        stringResource(R.string.settings_diagnostics_description),
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
                stringResource(R.string.settings_provider_requests_note),
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
                    stringResource(R.string.reset_vault_message)
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


private fun isHttpsEndpoint(value: String): Boolean = runCatching { Uri.parse(value).let { it.scheme == "https" && !it.host.isNullOrBlank() } }.getOrDefault(false)
