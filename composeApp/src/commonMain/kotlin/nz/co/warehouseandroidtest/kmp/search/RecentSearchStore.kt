package nz.co.warehouseandroidtest.kmp.search

import com.russhwolf.settings.Settings
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Remembers what has been searched for, most recent first.
 *
 * Stored as a JSON array in one key rather than indexed keys, so a read is atomic — an
 * interrupted write cannot leave a half-rewritten list behind.
 *
 * Nothing here is a secret, so it uses ordinary settings rather than the secure store.
 */
class RecentSearchStore(
    private val settings: Settings,
    private val limit: Int = DEFAULT_LIMIT,
) {

    fun read(): List<String> {
        val raw = settings.getStringOrNull(KEY) ?: return emptyList()
        // A corrupt value is not worth failing a screen over; treat it as no history.
        return runCatching { json.decodeFromString(serializer, raw) }.getOrDefault(emptyList())
    }

    /**
     * Adds [term] at the front. Re-searching something moves it up rather than duplicating it,
     * and the list is capped so history cannot grow without bound.
     */
    fun add(term: String): List<String> {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return read()

        val updated = (listOf(trimmed) + read().filterNot { it.equals(trimmed, ignoreCase = true) })
            .take(limit)
        settings.putString(KEY, json.encodeToString(serializer, updated))
        return updated
    }

    fun clear(): List<String> {
        settings.remove(KEY)
        return emptyList()
    }

    private companion object {
        const val KEY = "recentSearches"
        const val DEFAULT_LIMIT = 10
        val json = Json
        val serializer = ListSerializer(String.serializer())
    }
}
