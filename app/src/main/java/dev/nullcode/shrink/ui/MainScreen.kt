package dev.nullcode.shrink.ui

import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullcode.shrink.AppSettings
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

    Box(
        modifier
            .fillMaxSize()
            .background(c.background)
            .imePadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Text(
                "Make it short.",
                color = c.onBackground,
                fontSize = 40.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-1).sp,
            )
            Spacer(Modifier.height(28.dp))

            OutlinedTextField(
                value = input,
                onValueChange = { input = it; error = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 64.dp),
                singleLine = true,
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                placeholder = { Text("Paste a long link") },
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        TextButton(onClick = { input = ""; error = null; showResult = false }) {
                            Text("Clear", color = c.onSurfaceVariant)
                        }
                    }
                },
                shape = RoundedCornerShape(18.dp),
                colors = fieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() }),
            )

            AnimatedVisibility(input.isEmpty()) {
                Text(
                    "An empty box uses your clipboard.",
                    color = c.onSurfaceVariant,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 10.dp),
                )
            }

            Row(
                Modifier
                    .padding(top = 12.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { custom = !custom }
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Custom name",
                    color = c.onSurfaceVariant,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f),
                )
                Switch(checked = custom, onCheckedChange = { custom = it })
            }

            AnimatedVisibility(custom) {
                OutlinedTextField(
                    value = keyword,
                    onValueChange = { v ->
                        keyword = v.filter { ch -> ch.code < 128 && (ch.isLetterOrDigit() || ch == '-' || ch == '_') }.take(40)
                    },
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .fillMaxWidth(),
                    singleLine = true,
                    textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                    prefix = { Text("$host/", fontFamily = FontFamily.Monospace) },
                    placeholder = { Text("AbCdEf12") },
                    shape = RoundedCornerShape(14.dp),
                    colors = fieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Go),
                    keyboardActions = KeyboardActions(onGo = { submit() }),
                )
            }

            // Primary action
            val source = remember { MutableInteractionSource() }
            val pressed by source.collectIsPressedAsState()
            val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "press")
            Box(
                Modifier
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .height(56.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clip(RoundedCornerShape(50))
                    .background(c.primary)
                    .clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = !loading,
                        role = Role.Button,
                    ) { submit() },
                contentAlignment = Alignment.Center,
            ) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = c.onPrimary, strokeWidth = 2.5.dp)
                } else {
                    Text(
                        if (input.isBlank()) "Shorten clipboard" else "Shorten",
                        color = c.onPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            AnimatedVisibility(error != null) {
                Text(
                    error.orEmpty(),
                    color = c.error,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(start = 4.dp, top = 16.dp),
                )
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
                Column(
                    Modifier
                        .padding(top = 24.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.surface)
                        .clickable {
                            context.copyToClipboard(result)
                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                        }
                        .padding(20.dp)
                ) {
                    Text("Copied to clipboard. Tap to copy again.", color = c.onSurfaceVariant, fontSize = 13.sp)
                    Text(
                        result,
                        color = c.onSurface,
                        fontSize = 20.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    if (shorter) {
                        Box(
                            Modifier
                                .padding(top = 16.dp)
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(c.outline)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth(frac.value)
                                    .fillMaxHeight()
                                    .background(c.primary)
                            )
                        }
                        Text(
                            "${result.length} of $sourceLen characters",
                            color = c.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun fieldColors() = MaterialTheme.colorScheme.let { c ->
    OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.primary,
        unfocusedBorderColor = c.outline,
        focusedContainerColor = c.surface,
        unfocusedContainerColor = c.surface,
        cursorColor = c.primary,
        focusedTextColor = c.onSurface,
        unfocusedTextColor = c.onSurface,
        focusedPlaceholderColor = c.onSurfaceVariant,
        unfocusedPlaceholderColor = c.onSurfaceVariant,
        focusedPrefixColor = c.onSurfaceVariant,
        unfocusedPrefixColor = c.onSurfaceVariant,
    )
}
