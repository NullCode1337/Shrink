package dev.nullcode.shrink

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class LinkItem(
    val shortUrl: String,
    val longUrl: String,
    val title: String,
    val clicks: Int,
    val timestamp: String,
)

object Shortener {
    private val URL_RE = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val BARE_RE = Regex("""^[\w-]+(\.[\w-]+)+(/\S*)?$""")

    /** Pulls the first link out of arbitrary text (share sheets often send "Title https://..."). */
    fun extractUrl(raw: String?): String? {
        val t = raw?.trim().orEmpty()
        if (t.isEmpty()) return null
        URL_RE.find(t)?.let { return it.value.trimEnd('.', ',', ';', '!', '?') }
        if (BARE_RE.matches(t)) return "https://$t"
        return null
    }

    /** Returns the short URL. YOURLS answers with the existing short URL if the link was already shortened. */
    suspend fun shorten(endpoint: String, signature: String, url: String, keyword: String? = null): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (signature.isBlank()) {
                    error("No API signature set. Add it in Settings.")
                }
                val query = buildString {
                    append(endpoint)
                    append("?action=shorturl&format=json")
                    append("&signature=").append(URLEncoder.encode(signature, "UTF-8"))
                    append("&url=").append(URLEncoder.encode(url, "UTF-8"))
                    if (!keyword.isNullOrBlank()) {
                        append("&keyword=").append(URLEncoder.encode(keyword.trim(), "UTF-8"))
                    }
                }
                val conn = (URL(query).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                try {
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    val short = json?.optString("shorturl").orEmpty()
                    if (short.isNotEmpty()) {
                        short
                    } else {
                        error(json?.optString("message")?.takeIf { it.isNotEmpty() } ?: "Server returned HTTP $code")
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }

    /** Most recently created links, newest first. */
    suspend fun listLinks(endpoint: String, signature: String, limit: Int = 50): Result<List<LinkItem>> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (signature.isBlank()) {
                    error("No API signature set. Add it in Settings.")
                }
                val query = "$endpoint?action=stats&format=json" +
                    "&signature=${URLEncoder.encode(signature, "UTF-8")}" +
                    "&filter=last&limit=$limit"
                val conn = (URL(query).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                try {
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                        ?: error("Server returned HTTP $code")
                    val links = json.optJSONObject("links")
                        ?: return@runCatching emptyList()
                    links.keys().asSequence().mapNotNull { key ->
                        val o = links.optJSONObject(key) ?: return@mapNotNull null
                        LinkItem(
                            shortUrl = o.optString("shorturl"),
                            longUrl = o.optString("url"),
                            title = o.optString("title"),
                            clicks = o.optString("clicks").toIntOrNull() ?: 0,
                            timestamp = o.optString("timestamp"),
                        )
                    }.sortedByDescending { it.timestamp }.toList()
                } finally {
                    conn.disconnect()
                }
            }
        }

    /** Updates the long URL (and optionally title) for an existing short link.
     *  Requires the "API Edit URL" YOURLS plugin — core YOURLS has no update action. */
    suspend fun updateUrl(endpoint: String, signature: String, keyword: String, url: String, title: String? = null): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (signature.isBlank()) {
                    error("No API signature set. Add it in Settings.")
                }
                val query = buildString {
                    append(endpoint)
                    append("?action=update&format=json")
                    append("&signature=").append(URLEncoder.encode(signature, "UTF-8"))
                    append("&shorturl=").append(URLEncoder.encode(keyword, "UTF-8"))
                    append("&url=").append(URLEncoder.encode(url, "UTF-8"))
                    if (!title.isNullOrBlank()) {
                        append("&title=").append(URLEncoder.encode(title.trim(), "UTF-8"))
                    }
                }
                val conn = (URL(query).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                try {
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (!json.apiSucceeded()) {
                        error(json.failureMessage("Update needs the \"API Edit URL\" plugin on your YOURLS install."))
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }

    /** Deletes a short link entirely.
     *  Requires the "API Delete" YOURLS plugin — core YOURLS has no delete action. */
    suspend fun deleteUrl(endpoint: String, signature: String, keyword: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                if (signature.isBlank()) {
                    error("No API signature set. Add it in Settings.")
                }
                val query = buildString {
                    append(endpoint)
                    append("?action=delete&format=json")
                    append("&signature=").append(URLEncoder.encode(signature, "UTF-8"))
                    append("&shorturl=").append(URLEncoder.encode(keyword, "UTF-8"))
                }
                val conn = (URL(query).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 10_000
                }
                try {
                    val code = conn.responseCode
                    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                    val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
                    val json = runCatching { JSONObject(body) }.getOrNull()
                    if (!json.apiSucceeded()) {
                        error(json.failureMessage("Delete needs the \"API Delete\" plugin on your YOURLS install."))
                    }
                } finally {
                    conn.disconnect()
                }
            }
        }

    /** The keyword (short code) portion of a full short URL, e.g. "AbCdEf12" from ".../AbCdEf12". */
    fun keywordOf(shortUrl: String): String = shortUrl.trimEnd('/').substringAfterLast('/')

    private fun JSONObject?.apiSucceeded(): Boolean {
        if (this == null) return false
        if (optString("message").equals("success", ignoreCase = true)) return true
        val code = if (has("statusCode")) optInt("statusCode") else optInt("errorCode", -1)
        return code in 200..299
    }

    private fun JSONObject?.failureMessage(hint: String): String {
        val msg = this?.optString("message")?.takeIf { it.isNotEmpty() }
        return if (msg != null) "$msg. $hint" else hint
    }
}
