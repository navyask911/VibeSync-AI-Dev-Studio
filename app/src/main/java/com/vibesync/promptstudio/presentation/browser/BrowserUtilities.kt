package com.vibesync.promptstudio.presentation.browser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R
import kotlin.math.roundToInt

@Composable
internal fun BrowserSettingsSheet(
    desktopMode: Boolean,
    textZoom: Int,
    darkContent: Boolean,
    thirdPartyCookies: Boolean,
    onDesktopModeChange: (Boolean) -> Unit,
    onTextZoomChange: (Int) -> Unit,
    onDarkContentChange: (Boolean) -> Unit,
    onThirdPartyCookiesChange: (Boolean) -> Unit,
    onClearBrowsingData: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.browser_settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            SettingsSwitchRow(
                title = stringResource(R.string.browser_settings_user_agent),
                summary = stringResource(
                    if (desktopMode) R.string.browser_settings_desktop_mode
                    else R.string.browser_settings_mobile_mode
                ),
                checked = desktopMode,
                onCheckedChange = onDesktopModeChange
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.browser_settings_text_zoom),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.browser_settings_zoom_percent, textZoom),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = textZoom.toFloat(),
                    onValueChange = { onTextZoomChange(it.roundToInt().coerceIn(50, 200)) },
                    valueRange = 50f..200f,
                    steps = 29
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("50%", style = MaterialTheme.typography.labelSmall)
                    Text("100%", style = MaterialTheme.typography.labelSmall)
                    Text("200%", style = MaterialTheme.typography.labelSmall)
                }
            }
            SettingsSwitchRow(
                title = stringResource(R.string.browser_settings_dark_content),
                summary = stringResource(R.string.browser_settings_dark_content_summary),
                checked = darkContent,
                onCheckedChange = onDarkContentChange
            )
            SettingsSwitchRow(
                title = stringResource(R.string.browser_settings_third_party_cookies),
                summary = stringResource(R.string.browser_settings_third_party_cookies_summary),
                checked = thirdPartyCookies,
                onCheckedChange = onThirdPartyCookiesChange
            )
            Spacer(Modifier.height(2.dp))
            Button(onClick = onClearBrowsingData, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.browser_settings_clear_browsing_data))
            }
        }
    }
}

@Composable
private fun SettingsSwitchRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Text(summary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
internal fun ClearBrowsingDataDialog(
    onDismiss: () -> Unit,
    onConfirm: (cache: Boolean, cookies: Boolean, storage: Boolean, forms: Boolean) -> Unit
) {
    var clearCache by remember { mutableStateOf(true) }
    var clearCookies by remember { mutableStateOf(true) }
    var clearStorage by remember { mutableStateOf(true) }
    var clearForms by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(R.string.browser_settings_clear_browsing_data), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    stringResource(R.string.browser_settings_clear_data_explanation),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DataCheckboxRow(stringResource(R.string.browser_settings_clear_cache), clearCache) { clearCache = it }
                DataCheckboxRow(stringResource(R.string.browser_settings_clear_cookies), clearCookies) { clearCookies = it }
                DataCheckboxRow(stringResource(R.string.browser_settings_clear_storage), clearStorage) { clearStorage = it }
                DataCheckboxRow(stringResource(R.string.browser_settings_clear_forms), clearForms) { clearForms = it }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(clearCache, clearCookies, clearStorage, clearForms) },
                enabled = clearCache || clearCookies || clearStorage || clearForms
            ) {
                Text(stringResource(R.string.browser_settings_clear_selected))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DataCheckboxRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
