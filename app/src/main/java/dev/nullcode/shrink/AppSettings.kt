package dev.nullcode.shrink

import android.content.Context
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/**
 * Endpoint + signature, editable at runtime from the Settings tab.
 * BuildConfig values (from local.properties) are just the initial defaults.
 * Backed by SharedPreferences; the in-memory MutableStates keep every open
 * screen in sync the moment Settings is saved, with no restart needed.
 */
object AppSettings {
    private const val PREFS = "shrink_settings"
    private const val KEY_ENDPOINT = "endpoint"
    private const val KEY_SIGNATURE = "signature"

    private var endpointState: MutableState<String>? = null
    private var signatureState: MutableState<String>? = null

    fun endpoint(context: Context): MutableState<String> {
        ensureLoaded(context)
        return endpointState!!
    }

    fun signature(context: Context): MutableState<String> {
        ensureLoaded(context)
        return signatureState!!
    }

    /** Current values without subscribing to changes — safe to call from a coroutine. */
    fun current(context: Context): Pair<String, String> {
        ensureLoaded(context)
        return endpointState!!.value to signatureState!!.value
    }

    fun save(context: Context, endpoint: String, signature: String) {
        ensureLoaded(context)
        val cleanEndpoint = endpoint.trim().ifBlank { BuildConfig.YOURLS_ENDPOINT }
        val cleanSignature = signature.trim()
        prefs(context).edit()
            .putString(KEY_ENDPOINT, cleanEndpoint)
            .putString(KEY_SIGNATURE, cleanSignature)
            .apply()
        endpointState!!.value = cleanEndpoint
        signatureState!!.value = cleanSignature
    }

    fun defaultEndpoint(): String = BuildConfig.YOURLS_ENDPOINT
    fun defaultSignature(): String = BuildConfig.YOURLS_SIGNATURE

    private fun ensureLoaded(context: Context) {
        if (endpointState == null) {
            val p = prefs(context)
            endpointState = mutableStateOf(
                p.getString(KEY_ENDPOINT, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.YOURLS_ENDPOINT
            )
            signatureState = mutableStateOf(
                p.getString(KEY_SIGNATURE, null)?.takeIf { it.isNotBlank() } ?: BuildConfig.YOURLS_SIGNATURE
            )
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
