@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nullcode.shrink.ui

import android.text.format.DateUtils
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.nullcode.shrink.AppSettings
import dev.nullcode.shrink.LinkCache
import dev.nullcode.shrink.LinkItem
import dev.nullcode.shrink.OfflineException
import dev.nullcode.shrink.Shortener
import dev.nullcode.shrink.copyToClipboard
import kotlinx.coroutines.launch

@Composable
fun LinksScreen(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var refreshing by remember { mutableStateOf(true) }
    var hasData by remember { mutableStateOf(false) }      // true once cache or network produced a list (even an empty one)
    var error by remember { mutableStateOf<String?>(null) } // only surfaced when there is nothing to show
    var staleMsg by remember { mutableStateOf<String?>(null) } // refresh failed but a saved list is on screen
    var updatedAt by remember { mutableStateOf<Long?>(null) }
    var links by remember { mutableStateOf<List<LinkItem>>(emptyList()) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var editTarget by remember { mutableStateOf<LinkItem?>(null) }
    var deleteTarget by remember { mutableStateOf<LinkItem?>(null) }

    fun persist(list: List<LinkItem>) {
        scope.launch {
            val (endpoint, _) = AppSettings.current(context)
            LinkCache.save(context, endpoint, list, updatedAt ?: System.currentTimeMillis())
        }
    }

    LaunchedEffect(reloadKey) {
        refreshing = true
        error = null
        staleMsg = null
        val (endpoint, signature) = AppSettings.current(context)

        // Show the saved list immediately, then try to refresh it.
        if (!hasData) {
            LinkCache.load(context, endpoint)?.let {
                links = it.links
                updatedAt = it.savedAt
                hasData = true
            }
        }

        Shortener.listLinks(endpoint, signature)
            .onSuccess { fresh ->
                val now = System.currentTimeMillis()
                links = fresh
                updatedAt = now
                hasData = true
                LinkCache.save(context, endpoint, fresh, now)
            }
            .onFailure { e ->
                val msg = if (e is OfflineException) "You're offline" else e.message ?: "Couldn't refresh"
                if (hasData) staleMsg = msg else error = if (e is OfflineException) "You're offline. Nothing saved to show yet." else msg
            }
        refreshing = false
    }

    ShrinkScaffold(
        title = "Your links",
        modifier = modifier,
        actions = {
            IconButton(onClick = { reloadKey++ }, enabled = !refreshing) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
            }
        },
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            if (refreshing && hasData) {
                LinearWavyProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
            }

            staleMsg?.let { msg ->
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = c.secondaryContainer,
                    contentColor = c.onSecondaryContainer,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            msg + (updatedAt?.let { " · showing links saved ${ago(it)}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            when {
                !hasData && refreshing -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    LoadingIndicator()
                }

                !hasData && error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                        Icon(Icons.Filled.CloudOff, contentDescription = null, tint = c.onSurfaceVariant, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(error.orEmpty(), color = c.error, style = MaterialTheme.typography.bodyMedium)
                        TextButton(onClick = { reloadKey++ }) { Text("Retry") }
                    }
                }

                links.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Nothing shortened yet.", color = c.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
                }

                else -> LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    itemsIndexed(links, key = { _, item -> item.shortUrl }) { index, link ->
                        LinkRow(
                            link = link,
                            index = index,
                            count = links.size,
                            onCopy = {
                                context.copyToClipboard(link.shortUrl)
                                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                            },
                            onEdit = { editTarget = link },
                            onDelete = { deleteTarget = link },
                        )
                    }
                }
            }
        }
    }

    editTarget?.let { link ->
        EditLinkDialog(
            link = link,
            onDismiss = { editTarget = null },
            onSave = { newUrl, newTitle, onResult ->
                scope.launch {
                    val (endpoint, signature) = AppSettings.current(context)
                    val keyword = Shortener.keywordOf(link.shortUrl)
                    Shortener.updateUrl(endpoint, signature, keyword, newUrl, newTitle)
                        .onSuccess {
                            links = links.map {
                                if (it.shortUrl == link.shortUrl) it.copy(longUrl = newUrl, title = newTitle) else it
                            }
                            persist(links)
                            editTarget = null
                            Toast.makeText(context, "Updated", Toast.LENGTH_SHORT).show()
                        }
                        .onFailure { onResult(it.message ?: "Update failed.") }
                }
            },
        )
    }

    deleteTarget?.let { link ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete this link?") },
            text = { Text(shortLabel(link.shortUrl) + " will stop working immediately.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val (endpoint, signature) = AppSettings.current(context)
                        val keyword = Shortener.keywordOf(link.shortUrl)
                        Shortener.deleteUrl(endpoint, signature, keyword)
                            .onSuccess {
                                links = links.filterNot { it.shortUrl == link.shortUrl }
                                persist(links)
                                Toast.makeText(context, "Deleted", Toast.LENGTH_SHORT).show()
                            }
                            .onFailure {
                                Toast.makeText(context, it.message ?: "Delete failed.", Toast.LENGTH_LONG).show()
                            }
                        deleteTarget = null
                    }
                }) { Text("Delete", color = c.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun EditLinkDialog(
    link: LinkItem,
    onDismiss: () -> Unit,
    onSave: (url: String, title: String, onResult: (error: String) -> Unit) -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var url by remember { mutableStateOf(link.longUrl) }
    var title by remember { mutableStateOf(link.title) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        title = { Text("Edit ${shortLabel(link.shortUrl)}") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("URL") },
                    textStyle = monoStyle(MaterialTheme.typography.bodyMedium),
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("Title (optional)") },
                    shape = MaterialTheme.shapes.large,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error.orEmpty(), color = c.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = url.isNotBlank() && !saving,
                onClick = {
                    saving = true
                    error = null
                    onSave(url.trim(), title.trim()) { msg ->
                        saving = false
                        error = msg
                    }
                },
            ) { Text(if (saving) "Saving…" else "Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") }
        },
    )
}

@Composable
private fun LinkRow(
    link: LinkItem,
    index: Int,
    count: Int,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Surface(
        onClick = onCopy,
        shape = segmentShape(index, count),
        color = c.surfaceContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(c.primaryContainer, MaterialShapes.Cookie9Sided.toShape()),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Link, contentDescription = null, tint = c.onPrimaryContainer)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    shortLabel(link.shortUrl),
                    style = monoStyle(MaterialTheme.typography.titleMedium),
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    link.title.ifBlank { link.longUrl },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.width(8.dp))
            Surface(shape = CircleShape, color = c.secondaryContainer, contentColor = c.onSecondaryContainer) {
                Text(
                    "${link.clicks}",
                    style = monoStyle(MaterialTheme.typography.labelMedium),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More", tint = c.onSurfaceVariant)
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = c.error) },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = c.error) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

private fun shortLabel(shortUrl: String): String =
    shortUrl.removePrefix("https://").removePrefix("http://")

internal fun ago(millis: Long): String =
    DateUtils.getRelativeTimeSpanString(millis, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
