package com.vibesync.promptstudio.presentation.home

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import androidx.core.os.LocaleListCompat
import com.vibesync.promptstudio.R
import com.vibesync.promptstudio.presentation.devoptions.DeviceHardwareInfo
import com.vibesync.promptstudio.presentation.home.components.DevOptionsHubCard
import com.vibesync.promptstudio.presentation.home.components.HeroBrowserCard
import com.vibesync.promptstudio.presentation.home.components.ToolGridItem
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

private data class DashboardLocale(val tag: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeDashboardScreen(
    onOpenWebStudio: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onOpenPromptBank: () -> Unit,
    onOpenDeveloperResources: () -> Unit,
    onOpenToolbox: () -> Unit,
    onOpenApiStudio: () -> Unit,
    onOpenKeyVault: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onOpenCommands: () -> Unit,
    onOpenNetworkDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hardwareSnapshot by remember(context) { mutableStateOf(DeviceHardwareInfo.read(context)) }
    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hardwareSnapshot = DeviceHardwareInfo.read(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val appearancePrefs = remember(context) {
        context.getSharedPreferences("vibesync_appearance", android.content.Context.MODE_PRIVATE)
    }
    var appearance by remember(appearancePrefs) {
        mutableStateOf(appearancePrefs.getString("theme_mode", "normal") ?: "normal")
    }
    DisposableEffect(appearancePrefs) {
        val listener = android.content.SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == "theme_mode") {
                appearance = prefs?.getString("theme_mode", "normal") ?: "normal"
            }
        }
        appearancePrefs.registerOnSharedPreferenceChangeListener(listener)
        onDispose { appearancePrefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }
    var showThemeMenu by remember { mutableStateOf(false) }
    var showLanguageSheet by remember { mutableStateOf(false) }
    val languageSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locales = remember {
        listOf(
            DashboardLocale("en", "English"),
            DashboardLocale("es", "Español"),
            DashboardLocale("de", "Deutsch"),
            DashboardLocale("hi", "हिन्दी"),
            DashboardLocale("ar", "العربية")
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_launcher),
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = stringResource(R.string.app_name),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showThemeMenu = true }) {
                        Icon(Icons.Default.Palette, contentDescription = stringResource(R.string.home_theme))
                    }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_light)) },
                            leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) },
                            trailingIcon = { if (appearance == "normal") Text("✓", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                appearancePrefs.edit().putString("theme_mode", "normal").apply()
                                appearance = "normal"
                                showThemeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_system)) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null) },
                            trailingIcon = { if (appearance == "system") Text("✓", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                appearancePrefs.edit().putString("theme_mode", "system").apply()
                                appearance = "system"
                                showThemeMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.theme_matrix)) },
                            leadingIcon = { Icon(Icons.Default.Memory, contentDescription = null) },
                            trailingIcon = { if (appearance == "developer_dark") Text("✓", color = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                appearancePrefs.edit().putString("theme_mode", "developer_dark").apply()
                                appearance = "developer_dark"
                                showThemeMenu = false
                            }
                        )
                    }
                    IconButton(onClick = { showLanguageSheet = true }) {
                        Icon(Icons.Default.Language, contentDescription = stringResource(R.string.home_language))
                    }
                }
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.home_greeting),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        stringResource(R.string.home_dashboard_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { HeroBrowserCard(onClick = onOpenWebStudio) }
            item { SectionHeading(stringResource(R.string.home_section_device_system)) }
            item { DevOptionsHubCard(onClick = onOpenDeveloperOptions) }
            item { HardwareSnapshotCard(hardwareSnapshot) }

            item { SectionHeading(stringResource(R.string.home_section_developer_productivity)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(
                        stringResource(R.string.home_prompt_bank_title),
                        stringResource(R.string.home_prompt_bank_subtitle),
                        Icons.Default.AutoAwesome,
                        onOpenPromptBank,
                        Modifier.weight(1f)
                    )
                    ToolGridItem(
                        stringResource(R.string.home_resources_title),
                        stringResource(R.string.home_resources_subtitle),
                        Icons.Default.Code,
                        onOpenDeveloperResources,
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(
                        stringResource(R.string.home_toolbox_title),
                        stringResource(R.string.home_toolbox_subtitle),
                        Icons.Default.Storage,
                        onOpenToolbox,
                        Modifier.weight(1f)
                    )
                    ToolGridItem(
                        stringResource(R.string.home_api_title),
                        stringResource(R.string.home_api_subtitle),
                        Icons.Default.Api,
                        onOpenApiStudio,
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(
                        stringResource(R.string.home_vault_title),
                        stringResource(R.string.home_vault_subtitle),
                        Icons.Default.Lock,
                        onOpenKeyVault,
                        Modifier.weight(1f)
                    )
                    ToolGridItem(
                        stringResource(R.string.home_commands_title),
                        stringResource(R.string.home_commands_subtitle),
                        Icons.Default.Terminal,
                        onOpenCommands,
                        Modifier.weight(1f)
                    )
                }
            }
            item { SectionHeading(stringResource(R.string.home_section_diagnostics)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    ToolGridItem(
                        stringResource(R.string.home_diagnostics_title),
                        stringResource(R.string.home_diagnostics_subtitle),
                        Icons.Default.BugReport,
                        onOpenDiagnostics,
                        Modifier.weight(1f)
                    )
                    ToolGridItem(
                        stringResource(R.string.home_network_title),
                        stringResource(R.string.home_network_subtitle),
                        Icons.Default.Dns,
                        onOpenNetworkDiagnostics,
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                Text(
                    stringResource(R.string.home_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showLanguageSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLanguageSheet = false },
            sheetState = languageSheetState
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.home_choose_language),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_language_description),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                locales.forEach { locale ->
                    val appLanguageTags = AppCompatDelegate.getApplicationLocales().toLanguageTags()
                    val effectiveLanguageTags = appLanguageTags.ifBlank {
                        context.resources.configuration.locales[0].language
                    }
                    val selected = effectiveLanguageTags.split(',').any {
                        it.substringBefore('-') == locale.tag
                    }
                    Card(
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(locale.tag))
                            showLanguageSheet = false
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(15.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.secondaryContainer
                            else MaterialTheme.colorScheme.surfaceContainerLow
                        )
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(locale.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.weight(1f))
                            if (selected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun HardwareSnapshotCard(info: DeviceHardwareInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.devoptions_hardware_snapshot), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            HardwareRow(
                stringResource(R.string.devoptions_android_version),
                stringResource(R.string.devoptions_android_version_value, info.androidRelease, info.apiLevel)
            )
            HardwareRow(
                stringResource(R.string.devoptions_device_model),
                stringResource(R.string.devoptions_device_model_value, info.deviceModel, info.manufacturer, info.deviceCodename)
            )
            HardwareRow(stringResource(R.string.devoptions_security_patch), info.securityPatch)
            HardwareRow(stringResource(R.string.devoptions_kernel), info.kernelVersion)
            HardwareRow(stringResource(R.string.devoptions_cpu), "${info.socModel} · ${info.cpuAbi}")
            HardwareRow(
                stringResource(R.string.devoptions_refresh_rate),
                if (info.refreshRateHz > 0f) String.format(java.util.Locale.getDefault(), "%.1f Hz", info.refreshRateHz)
                else stringResource(R.string.devoptions_unavailable)
            )
            HardwareRow(
                stringResource(R.string.devoptions_ram),
                stringResource(
                    R.string.devoptions_ram_summary,
                    DeviceHardwareInfo.formatBytes(info.availableRamBytes),
                    DeviceHardwareInfo.formatBytes(info.totalRamBytes)
                )
            )
            HardwareRow(stringResource(R.string.devoptions_fingerprint), info.buildFingerprint)
        }
    }
}

@Composable
private fun HardwareRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
