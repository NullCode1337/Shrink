package dev.nullcode.shrink

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * On-disk copy of the last successful link list, so the Links tab still renders offline.
 * One JSON file in filesDir. Tagged with the endpoint it came from: pointing the app at a
 * different YOURLS instance never shows another instance's links. The API signature is never stored.
 */
object LinkCache {
    private const val FILE = "links_cache.json"
    private val lock = Any()

    data class Cached(val endpoint: String, val savedAt: Long, val links: List<LinkItem>)
    data class Info(val count: Int, val savedAt: Long)

    suspend fun load(context: Context, endpoint: String): Cached? = withContext(Dispatchers.IO) {
        read(context)?.takeIf { it.endpoint == endpoint }
    }

    suspend fun save(
        context: Context,
        endpoint: String,
        links: List<LinkItem>,
        savedAt: Long = System.currentTimeMillis(),
    ) = withContext(Dispatchers.IO) { write(context, Cached(endpoint, savedAt, links)) }

    /** Adds a freshly shortened link to an existing cache. No-op if nothing is cached yet or it's already there. */
    suspend fun add(context: Context, endpoint: String, shortUrl: String, longUrl: String) =
        withContext(Dispatchers.IO) {
            val cached = read(context)?.takeIf { it.endpoint == endpoint } ?: return@withContext
            if (cached.links.any { it.shortUrl == shortUrl }) return@withContext
            val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            write(context, cached.copy(links = listOf(LinkItem(shortUrl, longUrl, "", 0, stamp)) + cached.links))
        }

    suspend fun info(context: Context): Info? = withContext(Dispatchers.IO) {
        read(context)?.let { Info(it.links.size, it.savedAt) }
    }

    suspend fun clear(context: Context) = withContext(Dispatchers.IO) {
        synchronized(lock) { file(context).delete() }
        Unit
    }

    private fun file(context: Context) = File(context.filesDir, FILE)

    private fun read(context: Context): Cached? = synchronized(lock) {
        runCatching {
            val f = file(context)
            if (!f.exists()) return@runCatching null
            val root = JSONObject(f.readText())
            val arr = root.getJSONArray("links")
            val links = (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                LinkItem(
                    shortUrl = o.optString("shortUrl"),
                    longUrl = o.optString("longUrl"),
                    title = o.optString("title"),
                    clicks = o.optInt("clicks"),
                    timestamp = o.optString("timestamp"),
                )
            }
            Cached(root.getString("endpoint"), root.optLong("savedAt"), links)
        }.getOrNull()
    }

    private fun write(context: Context, cached: Cached) {
        synchronized(lock) {
            runCatching {
                val arr = JSONArray()
                cached.links.forEach {
                    arr.put(
                        JSONObject()
                            .put("shortUrl", it.shortUrl)
                            .put("longUrl", it.longUrl)
                            .put("title", it.title)
                            .put("clicks", it.clicks)
                            .put("timestamp", it.timestamp)
                    )
                }
                val json = JSONObject()
                    .put("endpoint", cached.endpoint)
                    .put("savedAt", cached.savedAt)
                    .put("links", arr)
                    .toString()
                val f = file(context)
                val tmp = File(f.parentFile, "$FILE.tmp")
                tmp.writeText(json)
                if (!tmp.renameTo(f)) {
                    f.delete()
                    tmp.renameTo(f)
                }
            }
        }
    }
}
