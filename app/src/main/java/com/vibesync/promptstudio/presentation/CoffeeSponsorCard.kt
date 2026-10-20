package com.vibesync.promptstudio.presentation

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vibesync.promptstudio.R

@Composable
fun CoffeeSponsorCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                stringResource(R.string.coffee_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.coffee_offline_badge),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                stringResource(R.string.coffee_message),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { openSupportLink(context, "https://github.com/sponsors") }) {
                    Text(stringResource(R.string.github_sponsors))
                }
                OutlinedButton(onClick = { openSupportLink(context, "https://www.buymeacoffee.com/") }) {
                    Text(stringResource(R.string.buy_me_coffee))
                }
            }
            Text(
                stringResource(R.string.coffee_footer),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openSupportLink(context: Context, url: String) {
    val uri = Uri.parse(url)
    try {
        CustomTabsIntent.Builder().build().launchUrl(context, uri)
    } catch (_: Exception) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
