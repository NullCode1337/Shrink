package dev.nullcode.shrink

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

/** Invisible share target: shorten, copy, toast, gone. */
class ShareActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val url = Shortener.extractUrl(intent?.getStringExtra(Intent.EXTRA_TEXT))
        if (url == null) {
            toast("No link found in what you shared")
            finish()
            return
        }

        lifecycleScope.launch {
            val (endpoint, signature) = AppSettings.current(applicationContext)
            Shortener.shorten(endpoint, signature, url)
                .onSuccess { short ->
                    copyToClipboard(short)
                    toast("Copied: $short")
                }
                .onFailure { toast(it.message ?: "Couldn't shorten the link") }
            finish()
        }
    }

    private fun toast(msg: String) =
        Toast.makeText(applicationContext, msg, Toast.LENGTH_SHORT).show()
}
