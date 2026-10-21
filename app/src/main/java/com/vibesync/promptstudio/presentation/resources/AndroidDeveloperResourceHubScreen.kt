package com.vibesync.promptstudio.presentation.resources

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Web
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import kotlinx.coroutines.launch

private data class LocalizedResourceRow(
    val resource: AndroidDeveloperResource,
    val title: String,
    val description: String
)

/** Searchable, localized catalog of curated Android developer resources. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AndroidDeveloperResourceHubScreen(
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val copiedMessage = stringResource(R.string.android_resource_copied_feedback)
    val shareChooser = stringResource(R.string.android_resource_share_chooser)
    val openFailedMessage = stringResource(R.string.developer_resource_open_failed)
    var search by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AndroidResourceCategory?>(null) }
    BackHandler(onBack = onBack)

    val resources = AndroidDeveloperResourceCatalog.resources.map { resource ->
        LocalizedResourceRow(
            resource = resource,
            title = stringResource(resource.titleRes),
            description = stringResource(resource.descriptionRes)
        )
    }
    val filtered = resources.filter { row ->
        (selectedCategory == null || row.resource.category == selectedCategory) &&
            (search.isBlank() || row.title.contains(search.trim(), ignoreCase = true) ||
                row.description.contains(search.trim(), ignoreCase = true) ||
                row.resource.url.contains(search.trim(), ignoreCase = true))
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.android_resource_hub_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        stringResource(R.string.android_resource_hub_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = search,
                        onValueChange = { search = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text(stringResource(R.string.android_resource_search)) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { selectedCategory = null },
                            label = { Text(stringResource(R.string.android_resource_category_all), maxLines = 1, softWrap = false) }
                        )
                        AndroidResourceCategory.entries.forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = if (selectedCategory == category) null else category },
                                label = { Text(stringResource(category.titleRes), maxLines = 1, softWrap = false) }
                            )
                        }
                    }
                }
            }
            items(filtered, key = { it.resource.id }) { row ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val categoryIcon = when (row.resource.category) {
                                AndroidResourceCategory.OFFICIAL_CORE -> Icons.Default.Code
                                AndroidResourceCategory.NEWSLETTERS_BLOGS -> Icons.Default.Web
                                AndroidResourceCategory.UTILITIES_PRODUCTIVITY -> Icons.Default.Terminal
                                AndroidResourceCategory.COMMUNITY_DISCUSSION -> Icons.Default.BugReport
                            }
                            Icon(categoryIcon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(
                                stringResource(row.resource.category.titleRes),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(row.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        SelectionContainer {
                            Text(
                                row.resource.url,
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                        Text(row.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(onClick = { onOpenUrl(row.resource.url) }) {
                                Icon(Icons.Default.Web, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(stringResource(R.string.android_resource_open))
                            }
                            IconButton(onClick = {
                                clipboard.setText(AnnotatedString(row.resource.url))
                                scope.launch { snackbarHostState.showSnackbar(copiedMessage) }
                            }) {
                                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.android_resource_copy_link))
                            }
                            IconButton(onClick = {
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, row.title)
                                    putExtra(Intent.EXTRA_TEXT, "${row.title}\n${row.resource.url}\n\n${row.description}")
                                }
                                runCatching { context.startActivity(Intent.createChooser(intent, shareChooser)) }
                                    .onFailure { Toast.makeText(context, openFailedMessage, Toast.LENGTH_LONG).show() }
                            }) {
                                Icon(Icons.Default.Share, contentDescription = stringResource(R.string.android_resource_share))
                            }
                        }
                    }
                }
            }
            if (filtered.isEmpty()) {
                item { Text(stringResource(R.string.android_resource_empty), modifier = Modifier.padding(12.dp)) }
            }
        }
    }
}
