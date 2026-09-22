package dev.nullcode.shrink.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullcode.shrink.AppSettings
import dev.nullcode.shrink.LinkItem
import dev.nullcode.shrink.Shortener
import dev.nullcode.shrink.copyToClipboard
import kotlinx.coroutines.launch

@Composable
fun LinksScreen(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var links by remember { mutableStateOf<List<LinkItem>>(emptyList()) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var editTarget by remember { mutableStateOf<LinkItem?>(null) }
    var deleteTarget by remember { mutableStateOf<LinkItem?>(null) }

    LaunchedEffect(reloadKey) {
        loading = true
        error = null
        val (endpoint, signature) = AppSettings.current(context)
        Shortener.listLinks(endpoint, signature)
            .onSuccess { links = it }
            .onFailure { error = it.message ?: "Couldn't load links." }
        loading = false
    }

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Your links",
                color = c.onBackground,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = { reloadKey++ }, enabled = !loading) {
                Icon(Icons.Filled.Refresh, contentDescription = "Refresh", tint = c.onSurfaceVariant)
            }
        }

        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = c.primary)
            }

            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error.orEmpty(), color = c.error, fontSize = 14.sp)
                    TextButton(onClick = { reloadKey++ }) { Text("Retry") }
                }
            }

            links.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Nothing shortened yet.", color = c.onSurfaceVariant, fontSize = 15.sp)
            }

            else -> LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, end = 24.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(links, key = { it.shortUrl }) { link ->
                    LinkRow(
                        link = link,
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
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    singleLine = true,
                    label = { Text("URL") },
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
                    shape = RoundedCornerShape(14.dp),
                    colors = editFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    singleLine = true,
                    label = { Text("Title (optional)") },
                    shape = RoundedCornerShape(14.dp),
                    colors = editFieldColors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                if (error != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(error.orEmpty(), color = c.error, fontSize = 13.sp)
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
private fun editFieldColors() = MaterialTheme.colorScheme.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.outline,
        cursorColor = c.primary,
    )
}

@Composable
private fun LinkRow(
    link: LinkItem,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = MaterialTheme.colorScheme
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(c.surface)
            .clickable(onClick = onCopy)
            .padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(c.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Link, contentDescription = null, tint = c.primary)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                shortLabel(link.shortUrl),
                color = c.onSurface,
                fontSize = 16.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                link.title.ifBlank { link.longUrl },
                color = c.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            "${link.clicks}",
            color = c.onSurfaceVariant,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
        )
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

private fun shortLabel(shortUrl: String): String =
    shortUrl.removePrefix("https://").removePrefix("http://")
