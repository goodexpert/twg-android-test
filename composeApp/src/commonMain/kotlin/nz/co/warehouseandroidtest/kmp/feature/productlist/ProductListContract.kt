package nz.co.warehouseandroidtest.kmp.feature.productlist

import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * Results for one search term.
 *
 * The list itself is not here yet — `getSearchResult` has not been ported — but [query] is,
 * because it arrives as a route argument and is what the screen is about.
 */
data class ProductListUiState(
    val query: String,
    val isLoading: Boolean = false,
) : UiState

sealed interface ProductListIntent : UiIntent {
    /** Pull to refresh, and the retry path once loading can fail. */
    data object Refresh : ProductListIntent
}
