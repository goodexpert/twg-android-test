package nz.co.warehouseandroidtest.kmp.feature.search

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import nz.co.warehouseandroidtest.kmp.search.RecentSearchStore

class SearchViewModelTest {

    private fun viewModel(store: RecentSearchStore = RecentSearchStore(MapSettings())) =
        SearchViewModel(store)

    @Test
    fun startsWithStoredHistory() {
        val store = RecentSearchStore(MapSettings())
        store.add("hammer")

        assertEquals(listOf("hammer"), viewModel(store).state.value.recentSearches)
    }

    @Test
    fun typingUpdatesTheQuery() {
        val viewModel = viewModel()

        viewModel.onIntent(SearchIntent.QueryChanged("drill"))

        assertEquals("drill", viewModel.state.value.query)
    }

    @Test
    fun submitRecordsTheTermInHistory() {
        val viewModel = viewModel()
        viewModel.onIntent(SearchIntent.QueryChanged("drill"))

        viewModel.onIntent(SearchIntent.Submit)

        assertEquals(listOf("drill"), viewModel.state.value.recentSearches)
    }

    @Test
    fun submitAsksTheScreenToOpenTheProductList() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(SearchIntent.QueryChanged("drill"))

        viewModel.onIntent(SearchIntent.Submit)

        assertEquals(SearchEffect.OpenProductList("drill"), viewModel.effects.first())
    }

    @Test
    fun submitTrimsTheTermBeforeNavigating() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(SearchIntent.QueryChanged("  drill  "))

        viewModel.onIntent(SearchIntent.Submit)

        assertEquals(SearchEffect.OpenProductList("drill"), viewModel.effects.first())
        assertEquals("drill", viewModel.state.value.query)
    }

    @Test
    fun blankSubmitNeitherRecordsNorNavigates() = runTest {
        val viewModel = viewModel()
        viewModel.onIntent(SearchIntent.QueryChanged("   "))

        viewModel.onIntent(SearchIntent.Submit)

        assertTrue(viewModel.state.value.recentSearches.isEmpty())
        // The screen must not navigate on a term the view model rejected.
        assertNull(withTimeoutOrNull(100) { viewModel.effects.first() })
    }

    @Test
    fun selectingHistorySearchesForItDirectly() = runTest {
        val store = RecentSearchStore(MapSettings())
        store.add("hammer")
        val viewModel = viewModel(store)

        viewModel.onIntent(SearchIntent.RecentSearchSelected("hammer"))

        assertEquals("hammer", viewModel.state.value.query)
        assertEquals(SearchEffect.OpenProductList("hammer"), viewModel.effects.first())
    }

    @Test
    fun clearingEmptiesStateAndStorage() {
        val store = RecentSearchStore(MapSettings())
        store.add("hammer")
        store.add("drill")
        val viewModel = viewModel(store)

        viewModel.onIntent(SearchIntent.RecentSearchesCleared)

        assertTrue(viewModel.state.value.recentSearches.isEmpty())
        assertTrue(store.read().isEmpty())
    }

    @Test
    fun canSubmitReflectsWhetherTheQueryHasContent() {
        val viewModel = viewModel()
        assertFalse(viewModel.state.value.canSubmit)

        viewModel.onIntent(SearchIntent.QueryChanged("drill"))

        assertTrue(viewModel.state.value.canSubmit)
    }
}
