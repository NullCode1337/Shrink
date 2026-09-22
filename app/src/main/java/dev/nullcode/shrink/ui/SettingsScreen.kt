package dev.nullcode.shrink.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.nullcode.shrink.AppSettings

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val c = MaterialTheme.colorScheme
    val context = LocalContext.current

    val endpointState = AppSettings.endpoint(context)
    val signatureState = AppSettings.signature(context)

    var endpoint by remember { mutableStateOf(endpointState.value) }
    var signature by remember { mutableStateOf(signatureState.value) }
    var reveal by remember { mutableStateOf(false) }

    val dirty = endpoint.trim() != endpointState.value || signature.trim() != signatureState.value

    Column(
        modifier
            .fillMaxSize()
            .background(c.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            "Settings",
            color = c.onBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
        )
        Spacer(Modifier.height(28.dp))

        Text("API endpoint", color = c.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        OutlinedTextField(
            value = endpoint,
            onValueChange = { endpoint = it },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            placeholder = { Text("https://your-domain/yourls-api.php") },
            shape = RoundedCornerShape(16.dp),
            colors = settingsFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Next),
        )

        Spacer(Modifier.height(20.dp))

        Text("Signature", color = c.onSurfaceVariant, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        OutlinedTextField(
            value = signature,
            onValueChange = { v -> signature = v.filter { !it.isWhitespace() } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            singleLine = true,
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 14.sp),
            placeholder = { Text("Paste your API signature") },
            visualTransformation = if (reveal) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { reveal = !reveal }) {
                    Icon(
                        if (reveal) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                        contentDescription = if (reveal) "Hide" else "Show",
                        tint = c.onSurfaceVariant,
                    )
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = settingsFieldColors(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
        )
        Text(
            "Found on your YOURLS Tools page, under API.",
            color = c.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp),
        )

        Spacer(Modifier.height(28.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            androidx.compose.material3.Button(
                onClick = {
                    AppSettings.save(context, endpoint, signature)
                    Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                },
                enabled = dirty,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text("Save", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.width(12.dp))
            TextButton(onClick = {
                endpoint = AppSettings.defaultEndpoint()
                signature = AppSettings.defaultSignature()
            }) {
                Text("Reset to build defaults", color = c.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun settingsFieldColors() = MaterialTheme.colorScheme.let { c ->
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
    )
}
