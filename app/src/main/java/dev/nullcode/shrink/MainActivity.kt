package dev.nullcode.shrink

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
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

                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = tab == 0,
                                onClick = { tab = 0 },
                                icon = { Icon(Icons.Filled.Link, contentDescription = null) },
                                label = { Text("Shorten") },
                            )
                            NavigationBarItem(
                                selected = tab == 1,
                                onClick = { tab = 1 },
                                icon = { Icon(Icons.Filled.List, contentDescription = null) },
                                label = { Text("Links") },
                            )
                            NavigationBarItem(
                                selected = tab == 2,
                                onClick = { tab = 2 },
                                icon = { Icon(Icons.Filled.Settings, contentDescription = null) },
                                label = { Text("Settings") },
                            )
                        }
                    }
                ) { innerPadding ->
                    when (tab) {
                        0 -> MainScreen(Modifier.padding(innerPadding))
                        1 -> LinksScreen(Modifier.padding(innerPadding))
                        else -> SettingsScreen(Modifier.padding(innerPadding))
                    }
                }
            }
        }
    }
}
