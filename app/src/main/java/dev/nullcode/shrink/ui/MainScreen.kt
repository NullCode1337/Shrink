@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nullcode.shrink.ui

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Link
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.nullcode.shrink.AppSettings
import dev.nullcode.shrink.LinkCache
import dev.nullcode.shrink.Shortener
import dev.nullcode.shrink.copyToClipboard
import dev.nullcode.shrink.readClipboardText
import kotlinx.coroutines.launch

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val endpointState = AppSettings.endpoint(context)
    val host = remember(endpointState.value) { Uri.parse(endpointState.value).host.orEmpty() }

    var input by rememberSaveable { mutableStateOf("") }
    var custom by rememberSaveable { mutableStateOf(false) }
    var keyword by rememberSaveable { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var showResult by rememberSaveable { mutableStateOf(false) }
    var result by rememberSaveable { mutableStateOf("") }
    var sourceLen by rememberSaveable { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }

    fun submit() {
        if (loading) return
        focus.clearFocus()
        error = null
        val fromClipboard = input.isBlank()
        val url = Shortener.extractUrl(if (fromClipboard) context.readClipboardText() else input)
        if (url == null) {
            showResult = false
            error = if (fromClipboard) "No link on the clipboard. Copy one first."
            else "That isn't a link. Check what you pasted."
            return
        }
        if (fromClipboard) input = url
        loading = true
        showResult = false
        scope.launch {
            val (endpoint, signature) = AppSettings.current(context)
            Shortener.shorten(endpoint, signature, url, keyword.takeIf { custom })
                .onSuccess { short ->
                    context.copyToClipboard(short)
                    LinkCache.add(context, endpoint, short, url)
                    result = short
                    sourceLen = url.length
                    showResult = true
                    // Android 13+ shows its own "copied" preview; older versions show nothing
                    if (Build.VERSION.SDK_INT < 33) {
                        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                    }
                }
                .onFailure { error = it.message ?: "Request failed. Check your connection." }
            loading = false
        }
    }

    ShrinkScaffold(title = "Make it short.", modifier = modifier) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .imePadding(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = 480.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it; error = null },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    textStyle = monoStyle(MaterialTheme.typography.bodyLarge),
                    placeholder = { Text("Paste a long link") },
                    leadingIcon = { Icon(Icons.Filled.Link, contentDescription = null) },
                    trailingIcon = {
                        if (input.isNotEmpty()) {
                            IconButton(onClick = { input = ""; error = null; showResult = false }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear")
                            }
                        }
                    },
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = shrinkFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                )

                AnimatedVisibility(input.isEmpty()) {
                    Text(
                        "An empty box uses your clipboard.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onSurfaceVariant,
                        modifier = Modifier.padding(start = 20.dp, top = 8.dp),
                    )
                }

                Surface(
                    onClick = { custom = !custom },
                    shape = MaterialTheme.shapes.large,
                    color = c.surfaceContainer,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "Custom name",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Switch(
                            checked = custom,
                            onCheckedChange = { custom = it },
                            thumbContent = if (custom) {
                                { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(SwitchDefaults.IconSize)) }
                            } else null,
                        )
                    }
                }

                AnimatedVisibility(custom) {
                    TextField(
                        value = keyword,
                        onValueChange = { v ->
                            keyword = v.filter { ch -> ch.code < 128 && (ch.isLetterOrDigit() || ch == '-' || ch == '_') }.take(40)
                        },
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .fillMaxWidth(),
                        singleLine = true,
                        textStyle = monoStyle(MaterialTheme.typography.bodyLarge),
                        prefix = { Text("$host/", fontFamily = FontFamily.Monospace) },
                        placeholder = { Text("AbCdEf12") },
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = shrinkFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go),
                        keyboardActions = KeyboardActions(onGo = { submit() }),
                    )
                }

                Button(
                    onClick = { submit() },
                    enabled = !loading,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    if (loading) {
                        LoadingIndicator(Modifier.size(32.dp), color = c.onPrimary)
                    } else {
                        Text(
                            if (input.isBlank()) "Shorten clipboard" else "Shorten",
                            style = MaterialTheme.typography.titleMediumEmphasized,
                        )
                    }
                }

                AnimatedVisibility(error != null) {
                    Surface(
                        shape = MaterialTheme.shapes.large,
                        color = c.errorContainer,
                        contentColor = c.onErrorContainer,
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                    ) {
                        Text(
                            error.orEmpty(),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }

                AnimatedVisibility(
                    showResult,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    val shorter = sourceLen > result.length
                    val frac = remember { Animatable(1f) }
                    LaunchedEffect(result) {
                        frac.snapTo(1f)
                        frac.animateTo(
                            (result.length.toFloat() / sourceLen.coerceAtLeast(1)).coerceIn(0.06f, 1f),
                            tween(750, easing = FastOutSlowInEasing),
                        )
                    }
                    Surface(
                        onClick = {
                            context.copyToClipboard(result)
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        },
                        shape = MaterialTheme.shapes.extraLarge,
                        color = c.primaryContainer,
                        contentColor = c.onPrimaryContainer,
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(24.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Copied to clipboard. Tap to copy again.",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.weight(1f),
                                )
                                Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                result,
                                style = monoStyle(MaterialTheme.typography.headlineSmallEmphasized),
                                modifier = Modifier.padding(top = 12.dp),
                            )
                            if (shorter) {
                                LinearWavyProgressIndicator(
                                    progress = { frac.value },
                                    modifier = Modifier
                                        .padding(top = 20.dp)
                                        .fillMaxWidth(),
                                    color = c.primary,
                                    trackColor = c.onPrimaryContainer.copy(alpha = 0.18f),
                                )
                                Text(
                                    "${result.length} of $sourceLen characters",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
