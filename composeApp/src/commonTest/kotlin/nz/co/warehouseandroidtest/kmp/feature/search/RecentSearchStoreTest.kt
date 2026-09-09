package nz.co.warehouseandroidtest.kmp.feature.search

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RecentSearchStoreTest {

    @Test
    fun readsBackWhatItWrote() {
        val store = RecentSearchStore(MapSettings())

        store.add("hammer")

        assertEquals(listOf("hammer"), store.read())
    }

    @Test
    fun mostRecentComesFirst() {
        val store = RecentSearchStore(MapSettings())

        store.add("hammer")
        store.add("drill")

        assertEquals(listOf("drill", "hammer"), store.read())
    }

    @Test
    fun repeatingASearchMovesItUpRatherThanDuplicating() {
        val store = RecentSearchStore(MapSettings())
        store.add("hammer")
        store.add("drill")

        store.add("hammer")

        assertEquals(listOf("hammer", "drill"), store.read())
    }

    @Test
    fun repeatIsCaseInsensitive() {
        val store = RecentSearchStore(MapSettings())
        store.add("Hammer")

        store.add("hammer")

        assertEquals(listOf("hammer"), store.read())
    }

    @Test
    fun trimsWhitespaceAndIgnoresBlankTerms() {
        val store = RecentSearchStore(MapSettings())

        store.add("  hammer  ")
        store.add("   ")

        assertEquals(listOf("hammer"), store.read())
    }

    @Test
    fun historyIsCapped() {
        val store = RecentSearchStore(MapSettings(), limit = 3)

        listOf("a", "b", "c", "d").forEach(store::add)

        assertEquals(listOf("d", "c", "b"), store.read())
    }

    @Test
    fun clearsEverything() {
        val store = RecentSearchStore(MapSettings())
        store.add("hammer")

        store.clear()

        assertTrue(store.read().isEmpty())
    }

    @Test
    fun treatsACorruptValueAsEmptyRatherThanThrowing() {
        val settings = MapSettings()
        settings.putString("recentSearches", "not json")

        assertTrue(RecentSearchStore(settings).read().isEmpty())
    }
}
