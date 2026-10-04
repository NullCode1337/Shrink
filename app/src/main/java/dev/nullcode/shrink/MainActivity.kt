@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package dev.nullcode.shrink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.nullcode.shrink.ui.LinksScreen
import dev.nullcode.shrink.ui.MainScreen
import dev.nullcode.shrink.ui.SettingsScreen
import dev.nullcode.shrink.ui.ShrinkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShrinkTheme {
                var tab by rememberSaveable { mutableIntStateOf(0) }
                val fade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

                Scaffold(
                    containerColor = MaterialTheme.colorScheme.background,
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = {
                                    Icon(
                                        if (tab == 0) Icons.Filled.Link else Icons.Outlined.Link,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text("Shorten") },
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = {
                                    Icon(
                                        if (tab == 1) Icons.AutoMirrored.Filled.List else Icons.AutoMirrored.Outlined.List,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text("Links") },
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                icon = {
                                    Icon(
                                        if (tab == 2) Icons.Filled.Settings else Icons.Outlined.Settings,
                                        contentDescription = null,
                                    )
                                },
                                label = { Text("Settings") },
                            )
                        }
                    }
                ) { innerPadding ->
                    // Each screen owns its top bar + status-bar inset; only the bottom bar padding comes from here.
                    val content = Modifier.padding(bottom = innerPadding.calculateBottomPadding())
                    AnimatedContent(
                        targetState = tab,
                        transitionSpec = { fadeIn(fade) togetherWith fadeOut(fade) },
                        label = "tab",
                    ) { current ->
                        when (current) {
                            0 -> MainScreen(content)
                            1 -> LinksScreen(content)
                            else -> SettingsScreen(content)
                        }
                    }
                }
            }
        }
    }
}
