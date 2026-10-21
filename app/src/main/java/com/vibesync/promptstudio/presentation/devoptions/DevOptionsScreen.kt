package com.vibesync.promptstudio.presentation.devoptions

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessibilityNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vibesync.promptstudio.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevOptionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var developerEnabled by remember(context) {
        mutableStateOf(DevSettingsHelper.isDeveloperModeEnabled(context))
    }
    var adbEnabled by remember(context) { mutableStateOf(DevSettingsHelper.isAdbEnabled(context)) }
    var showUnlockGuide by remember { mutableStateOf(false) }
    var snapshot by remember(context) { mutableStateOf(DeviceHardwareInfo.read(context)) }

    fun refreshStatus() {
        developerEnabled = DevSettingsHelper.isDeveloperModeEnabled(context)
        adbEnabled = DevSettingsHelper.isAdbEnabled(context)
        snapshot = DeviceHardwareInfo.read(context)
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refreshStatus()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    BackHandler(onBack = onBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.devoptions_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DeveloperMode, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                            Spacer(Modifier.size(10.dp))
                            Text(stringResource(R.string.devoptions_developer_mode), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        }
                        StatusPill(enabled = developerEnabled)
                        Text(
                            stringResource(R.string.devoptions_developer_mode_explanation),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(
                            onClick = {
                                if (!DevSettingsHelper.openDeveloperOptions(context)) showLaunchFailure(context)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.devoptions_open_settings))
                        }
                        OutlinedButton(
                            onClick = { showUnlockGuide = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.devoptions_unlock_guide))
                        }
                        HorizontalDivider()
                        Text(
                            text = stringResource(R.string.devoptions_adb_status, if (adbEnabled) stringResource(R.string.devoptions_on) else stringResource(R.string.devoptions_off)),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            item { SectionHeading(stringResource(R.string.devoptions_debugging_shortcuts)) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ShortcutCard(
                        title = stringResource(R.string.devoptions_usb_debugging),
                        subtitle = stringResource(R.string.devoptions_usb_subtitle),
                        icon = Icons.Default.Usb,
                        onClick = { if (!DevSettingsHelper.openUsbDebugging(context)) showLaunchFailure(context) },
                        modifier = Modifier.weight(1f)
                    )
                    ShortcutCard(
                        title = stringResource(R.string.devoptions_wireless_debugging),
                        subtitle = stringResource(R.string.devoptions_wireless_subtitle),
                        icon = Icons.Default.Wifi,
                        onClick = { if (!DevSettingsHelper.openWirelessDebugging(context)) showLaunchFailure(context) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            item {
                ShortcutCard(
                    title = stringResource(R.string.devoptions_app_standby),
                    subtitle = stringResource(R.string.devoptions_app_standby_subtitle),
                    icon = Icons.Default.BatteryChargingFull,
                    onClick = { if (!DevSettingsHelper.openAppStandby(context)) showLaunchFailure(context) }
                )
            }

            item { SectionHeading(stringResource(R.string.devoptions_system_settings)) }
            items(
                listOf(
                    ShortcutItem(R.string.devoptions_default_apps, R.string.devoptions_default_apps_subtitle, Icons.Default.Apps) { DevSettingsHelper.openDefaultApps(context) },
                    ShortcutItem(R.string.devoptions_overlay, R.string.devoptions_overlay_subtitle, Icons.Default.Security) { DevSettingsHelper.openOverlayPermission(context) },
                    ShortcutItem(R.string.devoptions_battery_optimization, R.string.devoptions_battery_optimization_subtitle, Icons.Default.BatteryChargingFull) { DevSettingsHelper.openBatteryOptimizationList(context) },
                    ShortcutItem(R.string.devoptions_notifications, R.string.devoptions_notifications_subtitle, Icons.Default.Notifications) { DevSettingsHelper.openAllAppNotifications(context) },
                    ShortcutItem(R.string.devoptions_accessibility, R.string.devoptions_accessibility_subtitle, Icons.Default.AccessibilityNew) { DevSettingsHelper.openAccessibility(context) }
                )
            ) { item ->
                ShortcutCard(
                    title = stringResource(item.titleRes),
                    subtitle = stringResource(item.subtitleRes),
                    icon = item.icon,
                    onClick = { if (!item.open()) showLaunchFailure(context) }
                )
            }

            item { SectionHeading(stringResource(R.string.devoptions_hardware_snapshot)) }
            item { HardwareSnapshotCard(snapshot) }
            item {
                Text(
                    stringResource(R.string.devoptions_limitations_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }

    if (showUnlockGuide) {
        AlertDialog(
            onDismissRequest = { showUnlockGuide = false },
            title = { Text(stringResource(R.string.devoptions_unlock_title)) },
            text = { Text(stringResource(R.string.devoptions_unlock_body)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showUnlockGuide = false
                        if (!DevSettingsHelper.openDeveloperOptions(context)) showLaunchFailure(context)
                    }
                ) { Text(stringResource(R.string.devoptions_open_settings)) }
            },
            dismissButton = {
                TextButton(onClick = { showUnlockGuide = false }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

private data class ShortcutItem(
    val titleRes: Int,
    val subtitleRes: Int,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val open: () -> Boolean
)

@Composable
private fun ShortcutCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun StatusPill(enabled: Boolean) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Text(
            text = stringResource(if (enabled) R.string.devoptions_enabled else R.string.devoptions_disabled),
            color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun HardwareSnapshotCard(info: DeviceHardwareInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            HardwareInfoRow(stringResource(R.string.devoptions_android_version), "Android ${info.androidRelease} · API ${info.apiLevel}")
            HorizontalDivider()
            HardwareInfoRow(stringResource(R.string.devoptions_kernel), info.kernelVersion)
            HorizontalDivider()
            HardwareInfoRow(stringResource(R.string.devoptions_cpu), "${info.socModel} · ${info.cpuAbi}")
            HorizontalDivider()
            HardwareInfoRow(
                stringResource(R.string.devoptions_refresh_rate),
                if (info.refreshRateHz > 0f) String.format(java.util.Locale.getDefault(), "%.1f Hz", info.refreshRateHz) else stringResource(R.string.devoptions_unavailable)
            )
            HorizontalDivider()
            HardwareInfoRow(
                stringResource(R.string.devoptions_ram),
                stringResource(
                    R.string.devoptions_ram_summary,
                    DeviceHardwareInfo.formatBytes(info.availableRamBytes),
                    DeviceHardwareInfo.formatBytes(info.totalRamBytes)
                )
            )
        }
    }
}

@Composable
private fun HardwareInfoRow(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

private fun showLaunchFailure(context: Context) {
    Toast.makeText(context, context.getString(R.string.devoptions_open_failed), Toast.LENGTH_LONG).show()
}
