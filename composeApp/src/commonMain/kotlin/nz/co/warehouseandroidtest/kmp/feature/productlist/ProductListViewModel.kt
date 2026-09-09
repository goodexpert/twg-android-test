package nz.co.warehouseandroidtest.kmp.feature.productlist

import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Takes the search term straight from the route, so the screen cannot exist without one.
 *
 * `Nothing` as the effect type: this screen has no one-shot events yet, and that makes
 * `sendEffect` uncallable rather than merely unused.
 */
class ProductListViewModel(
    query: String,
) : BaseViewModel<ProductListUiState, ProductListIntent, Nothing>(
    ProductListUiState(query = query),
) {

    override fun handleIntent(intent: ProductListIntent) {
        when (intent) {
            // Loading lands here once getSearchResult is ported, together with paging,
            // pull-to-refresh and the footer states the legacy adapter tracked.
            ProductListIntent.Refresh -> Unit
        }
    }
}
