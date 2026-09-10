package nz.co.warehouseandroidtest.kmp.feature.productlist

import nz.co.warehouseandroidtest.kmp.data.ErrorState
import nz.co.warehouseandroidtest.kmp.ui.UiEffect
import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * The two ways the screen lays out its results.
 *
 * Held in state rather than as a local `remember` so the choice survives a rotation or a
 * re-collect on return, and so tests can drive the toggle without touching the view.
 */
enum class ProductListLayout { List, Grid }

/**
 * Results for one search term, growing as the user scrolls.
 *
 * [items] is the accumulator, not one page: `Refresh` replaces it and `LoadMore` appends to
 * it, so the screen only ever renders one continuous list.
 *
 * [loadedCount] and [items] are separate on purpose. [items] is the distinct list the
 * composable renders — the search endpoint can return the same `productId` on more than one
 * page (a promoted product, or a sort tiebreak that moves between requests), so the ViewModel
 * dedupes on append and only unique items go here. [loadedCount] is what the server has
 * actually sent us across all pages, dupes included, and it is the cursor the next page
 * request uses. Using `items.size` for that would slowly drift behind the server on any
 * response with a duplicate — pages would overlap and, if the drift kept up, the fetch loop
 * would never terminate.
 *
 * [totalCount] is the ceiling the server reported for the whole result set — [canLoadMore]
 * compares [loadedCount] against it to decide when to stop.
 *
 * One [isLoading] and one [error] rather than a separate pair for paging: the split
 * initial-load vs paging is derived from `items.isEmpty()` in the composable rather than from
 * a second flag. `isLoading && items.isEmpty()` renders as a full-page spinner,
 * `isLoading && items.isNotEmpty()` renders as a footer spinner underneath the list — same
 * rule for [error]. This rests on the invariant that `Refresh` runs only while [items] is
 * empty (its only entry point is the `ErrorContent` retry, which is only shown when items
 * are empty). Add a separate `isRefreshing` flag if pull-to-refresh ever needs to fire
 * against a populated list.
 */
data class ProductListUiState(
    val query: String,
    val isLoading: Boolean = false,
    val layout: ProductListLayout = ProductListLayout.List,
    val items: List<ProductCardData> = emptyList(),
    val loadedCount: Int = 0,
    val totalCount: Int = 0,
    val error: ErrorState? = null,
) : UiState {

    /**
     * True when another page can be requested. The ViewModel guards on the same two
     * conditions — this property is the single sentence the composable can read to decide
     * whether the scroll trigger should fire at all.
     *
     * Deliberately does not include [error]: a failed page can be retried, so the "can more
     * be loaded" question and the "should we auto-fetch right now" question are different.
     * The composable combines them, the ViewModel does not.
     */
    val canLoadMore: Boolean
        get() = !isLoading && loadedCount < totalCount
}

sealed interface ProductListIntent : UiIntent {
    /** Pull to refresh, and the retry path when a full-page load fails. */
    data object Refresh : ProductListIntent

    /**
     * Fetch the next page. Dispatched by the scroll trigger and by the footer retry — the
     * ViewModel treats both the same and drops the call when there is nothing to fetch or a
     * request is already in flight.
     */
    data object LoadMore : ProductListIntent

    /** Switch between grid and list. Driven from the toggle on the product-count row. */
    data class LayoutChanged(val layout: ProductListLayout) : ProductListIntent

    /** A card in the list or grid was tapped. Carries the id the details route needs. */
    data class OnCardClicked(val productId: String) : ProductListIntent
}

sealed interface ProductListEffect : UiEffect {
    /** Navigate to the product details screen for [productId]. */
    data class OpenProductDetails(val productId: String) : ProductListEffect
}
