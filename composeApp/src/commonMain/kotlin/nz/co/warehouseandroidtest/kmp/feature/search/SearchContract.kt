package nz.co.warehouseandroidtest.kmp.feature.search

import nz.co.warehouseandroidtest.kmp.ui.UiEffect
import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * Everything the search screen renders: the query being typed and the history below it.
 *
 * Results are not here — they belong to the product list, which is a separate destination
 * reached with the term as a route argument.
 */
data class SearchUiState(
    val query: String = "",
    val recentSearches: List<String> = emptyList(),
) : UiState {

    val canSubmit: Boolean get() = query.isNotBlank()
}

sealed interface SearchIntent : UiIntent {
    data class QueryChanged(val query: String) : SearchIntent

    /** Enter on the toolbar field. */
    data object Submit : SearchIntent

    /** Tapping a row in the history: searches for it directly. */
    data class RecentSearchSelected(val term: String) : SearchIntent

    data object RecentSearchesCleared : SearchIntent
}

sealed interface SearchEffect : UiEffect {
    /**
     * Emitted only for a term the view model accepted, so the screen never navigates on a
     * blank query — the decision stays in one place.
     */
    data class OpenProductList(val query: String) : SearchEffect
}
