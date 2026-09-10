package nz.co.warehouseandroidtest.kmp.feature.search

import nz.co.warehouseandroidtest.kmp.search.RecentSearchStore
import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Named [store] rather than `recentSearches`: inside `setState { copy(...) }` the receiver is
 * the state, so that name would resolve to the state's own list instead.
 */
class SearchViewModel(
    private val store: RecentSearchStore,
) : BaseViewModel<SearchUiState, SearchIntent, SearchEffect>(
    SearchUiState(recentSearches = store.read()),
) {

    override fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> setState { copy(query = intent.query) }

            SearchIntent.Submit -> submit(currentState.query)

            is SearchIntent.RecentSearchSelected -> submit(intent.term)

            SearchIntent.RecentSearchesCleared -> {
                val updated = store.clear()
                setState { copy(recentSearches = updated) }
            }
        }
    }

    /**
     * Records the term, then asks the screen to open the product list. Both entry points —
     * pressing enter and tapping history — come through here, so the blank check and the
     * history write cannot get out of step between them.
     */
    private fun submit(term: String) {
        val trimmed = term.trim()
        if (trimmed.isEmpty()) return

        val updated = store.add(trimmed)
        setState { copy(query = "", recentSearches = updated) }
        sendEffect(SearchEffect.OpenProductList(trimmed))
    }
}
