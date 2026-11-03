package com.haxzie.relaybird.update

import android.content.Context
import com.haxzie.relaybird.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

/**
 * Tells the user when a newer release exists. It does not install anything.
 *
 * The app is sideloaded, so nothing updates it automatically, and the two halves are gated
 * against each other by protocol version — the Mac updates itself and can walk into refusing
 * to talk to a phone that never heard about the new release. This closes that silently-stops-
 * working case without asking for `REQUEST_INSTALL_PACKAGES`, which is a heavier permission
 * than a notice is worth.
 *
 * The feed is the same `latest.json` the website reads. Both apps are released together from
 * one changeset group, so its version is the Android version too.
 */
class UpdateChecker(context: Context) {

    @Serializable
    private data class Feed(val version: String = "", val publishedAt: String = "")

    data class Available(val version: String)

    /** Null until a check finds something newer; the UI shows a card when it is set. */
    val available = MutableStateFlow<Available?>(null)

    private val prefs = context.getSharedPreferences("relaybird_update", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun checkIfDue(now: Long = System.currentTimeMillis()) {
        val last = prefs.getLong(KEY_LAST_CHECK, 0L)
        if (now - last < CHECK_INTERVAL_MS) {
            // Still show a find from an earlier check, so the card survives a restart.
            prefs.getString(KEY_LAST_SEEN, null)?.let { seen ->
                if (isNewer(seen, BuildConfig.VERSION_NAME)) available.value = Available(seen)
            }
            return
        }

        val latest = fetch() ?: return
        prefs.edit().putLong(KEY_LAST_CHECK, now).putString(KEY_LAST_SEEN, latest).apply()
        available.value = if (isNewer(latest, BuildConfig.VERSION_NAME)) Available(latest) else null
    }

    private suspend fun fetch(): String? = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(FEED_URL).openConnection() as HttpURLConnection).apply {
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                setRequestProperty("Accept", "application/json")
            }
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                json.decodeFromString(Feed.serializer(), body).version.ifBlank { null }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            // Offline, DNS not resolving yet, malformed feed: all the same outcome. An update
            // notice that fails is not worth surfacing as an error.
            null
        }
    }

    internal companion object {
        const val FEED_URL = "https://assets.haxzie.com/relaybird/latest.json"
        const val RELEASES_URL = "https://github.com/haxzie/relay-bird/releases"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val KEY_LAST_SEEN = "last_seen_version"
        private const val CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L
        private const val TIMEOUT_MS = 8_000

        /**
         * Semver compare, numeric per component.
         *
         * String comparison gets this wrong in the obvious place: "1.10.0" sorts before
         * "1.9.0" lexically, so the app would stop offering updates after the tenth patch.
         * Anything unparseable is treated as not newer rather than nagging on bad data.
         */
        fun isNewer(candidate: String, current: String): Boolean {
            val a = parse(candidate) ?: return false
            val b = parse(current) ?: return false
            for (i in 0 until maxOf(a.size, b.size)) {
                val l = a.getOrElse(i) { 0 }
                val r = b.getOrElse(i) { 0 }
                if (l != r) return l > r
            }
            return false
        }

        private fun parse(version: String): List<Int>? {
            val core = version.trim().removePrefix("v").substringBefore('-').substringBefore('+')
            if (core.isEmpty()) return null
            return core.split('.').map { it.toIntOrNull() ?: return null }
        }
    }
}
