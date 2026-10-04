@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nullcode.shrink.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import dev.nullcode.shrink.AppSettings
import dev.nullcode.shrink.LinkCache
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val endpointState = AppSettings.endpoint(context)
    val signatureState = AppSettings.signature(context)

    var endpoint by remember { mutableStateOf(endpointState.value) }
    var signature by remember { mutableStateOf(signatureState.value) }
    var reveal by remember { mutableStateOf(false) }
    var cacheInfo by remember { mutableStateOf<LinkCache.Info?>(null) }

    LaunchedEffect(Unit) { cacheInfo = LinkCache.info(context) }

    val dirty = endpoint.trim() != endpointState.value || signature.trim() != signatureState.value

    ShrinkScaffold(title = "Settings", modifier = modifier) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                "YOURLS",
                style = MaterialTheme.typography.titleSmall,
                color = c.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )

            TextField(
                value = endpoint,
                onValueChange = { endpoint = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("API endpoint") },
                textStyle = monoStyle(MaterialTheme.typography.bodyMedium),
                placeholder = { Text("https://your-domain/yourls-api.php") },
                shape = MaterialTheme.shapes.extraLarge,
                colors = shrinkFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
            )

            Spacer(Modifier.height(8.dp))

            TextField(
                value = signature,
                onValueChange = { v -> signature = v.filter { !it.isWhitespace() } },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("Signature") },
                textStyle = monoStyle(MaterialTheme.typography.bodyMedium),
                placeholder = { Text("Paste your API signature") },
                supportingText = { Text("Found on your YOURLS Tools page, under API.") },
                visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { reveal = !reveal }) {
                        Icon(
                            if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (reveal) "Hide" else "Show",
                        )
                    }
                },
                shape = MaterialTheme.shapes.extraLarge,
                colors = shrinkFieldColors(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            )

            Spacer(Modifier.height(20.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = {
                        AppSettings.save(context, endpoint, signature)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                    },
                    enabled = dirty,
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.height(48.dp),
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLargeEmphasized)
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = {
                    endpoint = AppSettings.defaultEndpoint()
                    signature = AppSettings.defaultSignature()
                }) {
                    Text("Reset to build defaults")
                }
            }

            Spacer(Modifier.height(32.dp))

            Text(
                "OFFLINE",
                style = MaterialTheme.typography.titleSmall,
                color = c.primary,
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp),
            )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                color = c.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    Modifier.padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Saved links", style = MaterialTheme.typography.titleMedium)
                        Text(
                            cacheInfo?.let { "${it.count} links · updated ${ago(it.savedAt)}" } ?: "Nothing saved yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.onSurfaceVariant,
                        )
                    }
                    TextButton(
                        enabled = cacheInfo != null,
                        onClick = {
                            scope.launch {
                                LinkCache.clear(context)
                                cacheInfo = null
                            }
                        },
                    ) { Text("Clear") }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
