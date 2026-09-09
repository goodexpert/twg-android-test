package nz.co.warehouseandroidtest.kmp.feature.productlist

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.kmp.network.toErrorState
import nz.co.warehouseandroidtest.kmp.search.SearchRepository
import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Runs the search for the term the route carried in and drives infinite-scroll paging on top
 * of it.
 *
 * Two paths through the repository, sharing one query: [search] fetches page 0 and replaces
 * the list, [loadMore] fetches at `start = loadedCount` and appends. `loadedCount` is what
 * the server has actually sent us — dupes included — so the paging cursor cannot drift when
 * the endpoint returns the same product on more than one page. Dedup happens on append, so
 * [ProductListUiState.items] stays distinct (which is what the LazyColumn's `key = { it.id }`
 * needs to stay unique).
 *
 * A single [ProductListUiState.isLoading] flag serves both fetches: the composable
 * distinguishes them by whether [ProductListUiState.items] is empty. Same for
 * [ProductListUiState.error]. See the contract for why the initial/paging split is derived
 * rather than stored.
 *
 * `Nothing` as the effect type: this screen has no one-shot events yet, and that makes
 * `sendEffect` uncallable rather than merely unused.
 */
class ProductListViewModel(
    private val query: String,
    private val searchRepository: SearchRepository,
    private val preferencesStore: ProductListPreferencesStore,
) : BaseViewModel<ProductListUiState, ProductListIntent, Nothing>(
    // Read once at construction: the store is a plain settings read, and lifting the last
    // choice into the initial state keeps the toggle's answer consistent from the first
    // frame the screen paints.
    ProductListUiState(query = query, layout = preferencesStore.readLayout()),
) {

    init {
        search()
    }

    override fun handleIntent(intent: ProductListIntent) {
        when (intent) {
            ProductListIntent.Refresh -> search()

            ProductListIntent.LoadMore -> loadMore()

            is ProductListIntent.LayoutChanged -> {
                preferencesStore.writeLayout(intent.layout)
                setState { copy(layout = intent.layout) }
            }
        }
    }

    private fun search() {
        viewModelScope.launch {
            // Clears the error so a stale message does not sit next to the spinner during a
            // retry.
            setState { copy(isLoading = true, error = null) }

            searchRepository.search(query, start = 0)
                .onSuccess { response ->
                    setState {
                        // distinctBy against the response itself, in case a single page comes
                        // back with a repeated productId — the LazyColumn key would refuse it.
                        val page = response.products
                            .map { it.toProductCardData() }
                            .distinctBy { it.id }
                        copy(
                            isLoading = false,
                            items = page,
                            loadedCount = response.products.size,
                            totalCount = response.total,
                        )
                    }
                }
                .onFailure { throwable ->
                    setState {
                        copy(isLoading = false, error = throwable.toErrorState())
                    }
                }
        }
    }

    private fun loadMore() {
        val current = currentState
        // Guards, in the order that matters: a request is already in flight, or the ceiling
        // has been reached. The second guard also drops the very first LoadMore before a page
        // exists (totalCount is still 0), so init and paging cannot collide on start-up.
        if (current.isLoading) return
        if (current.loadedCount >= current.totalCount) return

        viewModelScope.launch {
            // Clearing the error here is what makes the footer's retry button a plain
            // LoadMore dispatch — no separate intent needed.
            setState { copy(isLoading = true, error = null) }

            searchRepository.search(query, start = current.loadedCount)
                .onSuccess { response ->
                    setState {
                        val known = items.mapTo(mutableSetOf()) { it.id }
                        val added = response.products
                            .map { it.toProductCardData() }
                            // Set.add returns true only when the id is new to `known`, so
                            // dupes within the response and dupes against already-loaded
                            // items are dropped in the same pass.
                            .filter { known.add(it.id) }
                        copy(
                            isLoading = false,
                            items = items + added,
                            // Increment by what the server sent, not by what we kept — the
                            // cursor tracks the server's window, not the display list.
                            loadedCount = loadedCount + response.products.size,
                            // Total can move under paging (products going out of stock between
                            // pages), so re-read it rather than trust the value the first page
                            // reported.
                            totalCount = response.total,
                        )
                    }
                }
                .onFailure { throwable ->
                    setState {
                        copy(isLoading = false, error = throwable.toErrorState())
                    }
                }
        }
    }
}
