package dev.nullcode.shrink

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

fun Context.copyToClipboard(text: String) {
    getSystemService(ClipboardManager::class.java)
        .setPrimaryClip(ClipData.newPlainText("short link", text))
}

/** Only works while the app is in the foreground (Android 10+). */
fun Context.readClipboardText(): String? =
    getSystemService(ClipboardManager::class.java).primaryClip
        ?.takeIf { it.itemCount > 0 }
        ?.getItemAt(0)
        ?.coerceToText(this)
        ?.toString()
