package nz.co.warehouseandroidtest.kmp.feature.productlist

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nz.co.warehouseandroidtest.kmp.data.ErrorState
import nz.co.warehouseandroidtest.kmp.data.PriceInfo
import nz.co.warehouseandroidtest.kmp.data.Product
import nz.co.warehouseandroidtest.kmp.data.SearchResponse
import nz.co.warehouseandroidtest.kmp.data.User
import nz.co.warehouseandroidtest.kmp.network.FakeWarehouseApi
import nz.co.warehouseandroidtest.kmp.search.SEARCH_PAGE_SIZE
import nz.co.warehouseandroidtest.kmp.search.SearchRepository
import nz.co.warehouseandroidtest.kmp.session.SessionRepository
import nz.co.warehouseandroidtest.kmp.session.SessionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `viewModelScope` runs on `Dispatchers.Main`, which `runTest` does not drive by default. Same
 * pattern as `HomeViewModelTest` — [setMain] lands `init`'s launch on the test scheduler so
 * `advanceUntilIdle` decides when it runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductListViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val loggedInUser = User(customerId = "cust-123", expiryMinutes = 30)

    private fun page(startId: Int, size: Int, total: Int) = SearchResponse(
        products = List(size) {
            Product(
                productId = "R${startId + it}",
                productName = "Item ${startId + it}",
                priceInfo = PriceInfo(price = 9.99),
            )
        },
        searchTerm = "Paper",
        total = total,
    )

    @BeforeTest
    fun substituteMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun api(
        search: Result<SearchResponse> = Result.success(page(0, SEARCH_PAGE_SIZE, 200)),
        login: Result<User> = Result.success(loggedInUser),
        gate: CompletableDeferred<Unit> = CompletableDeferred(Unit),
    ) = FakeWarehouseApi(loginResult = login, searchResult = search, gate = gate)

    private fun searchRepository(api: FakeWarehouseApi): SearchRepository = SearchRepository(
        api = api,
        sessionRepository = SessionRepository(api, SessionStore(MapSettings()), now = { 1_000L }),
    )

    private fun preferencesStore(settings: MapSettings = MapSettings()) =
        ProductListPreferencesStore(settings)

    private fun viewModel(
        query: String = "Paper",
        api: FakeWarehouseApi = api(),
        preferencesStore: ProductListPreferencesStore = preferencesStore(),
    ) = ProductListViewModel(
        query = query,
        searchRepository = searchRepository(api),
        preferencesStore = preferencesStore,
    )

    @Test
    fun takesItsQueryFromTheRoute() = runTest {
        val vm = viewModel(query = "hammer", api = api(search = Result.success(SearchResponse())))
        advanceUntilIdle()

        assertEquals("hammer", vm.state.value.query)
    }

    @Test
    fun defaultsToListLayoutWhenNothingIsPersisted() {
        assertEquals(ProductListLayout.List, viewModel().state.value.layout)
    }

    @Test
    fun readsThePersistedLayoutOnConstruction() {
        val settings = MapSettings()
        preferencesStore(settings).writeLayout(ProductListLayout.Grid)

        val vm = viewModel(preferencesStore = preferencesStore(settings))

        assertEquals(ProductListLayout.Grid, vm.state.value.layout)
    }

    @Test
    fun layoutChangedUpdatesStateAndPersists() = runTest {
        val settings = MapSettings()
        val store = preferencesStore(settings)
        val vm = viewModel(preferencesStore = store)
        advanceUntilIdle()
        val before = vm.state.value

        vm.onIntent(ProductListIntent.LayoutChanged(ProductListLayout.Grid))

        val after = vm.state.value
        assertEquals(ProductListLayout.Grid, after.layout)
        // The rest of the state is untouched — the toggle only carries a layout choice.
        assertEquals(before.query, after.query)
        assertEquals(before.items, after.items)
        assertEquals(before.totalCount, after.totalCount)
        // A fresh store reading the same settings sees the write — this is what makes the
        // choice survive a process restart.
        assertEquals(ProductListLayout.Grid, preferencesStore(settings).readLayout())
    }

    @Test
    fun searchesForTheRouteQueryOnInit() = runTest {
        val api = api()

        viewModel(query = "Paper", api = api)
        advanceUntilIdle()

        assertEquals(1, api.searchCallCount)
        assertEquals("Paper", api.lastSearchRequest?.query)
        assertEquals(0, api.lastSearchRequest?.start)
    }

    @Test
    fun populatesItemsAndTotalFromTheResponse() = runTest {
        val vm = viewModel(api = api(search = Result.success(page(0, 20, 200))))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals(200, state.totalCount)
        assertEquals(20, state.items.size)
        assertEquals(20, state.loadedCount)
        assertEquals("R0", state.items[0].id)
    }

    @Test
    fun surfacesErrorStateWhenTheSearchFails() = runTest {
        // A bare RuntimeException does not carry a status, so it maps to Unknown rather than
        // being dressed up as a connection error — see toErrorState's fallback.
        val vm = viewModel(api = api(search = Result.failure(RuntimeException("boom"))))
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(ErrorState.UnknownErrorState, state.error)
        assertTrue(state.items.isEmpty())
    }

    @Test
    fun refreshRunsAnotherSearchAtOffsetZero() = runTest {
        val api = api()
        val vm = viewModel(query = "Paper", api = api)
        advanceUntilIdle()
        assertEquals(1, api.searchCallCount)

        vm.onIntent(ProductListIntent.Refresh)
        advanceUntilIdle()

        assertEquals(2, api.searchCallCount)
        assertEquals(0, api.lastSearchRequest?.start)
    }

    @Test
    fun loadMoreAppendsTheNextPageAtTheRightOffset() = runTest {
        // First page: ids R0..R19; second: R20..R39.
        val api = api(search = Result.success(page(startId = 0, size = 20, total = 200)))
        val vm = viewModel(api = api)
        advanceUntilIdle()
        assertEquals(20, vm.state.value.items.size)

        api.searchResult = Result.success(page(startId = 20, size = 20, total = 200))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(40, state.items.size)
        assertEquals(40, state.loadedCount)
        assertEquals("R20", state.items[20].id)
        // Offset is what the server has already sent us (loadedCount), not the size of the
        // display list. The two are the same here because these pages have no duplicates.
        assertEquals(20, api.lastSearchRequest?.start)
    }

    @Test
    fun dedupesProductsThatAppearInMoreThanOnePage() = runTest {
        // A promoted product that appears in every response would otherwise crash the
        // LazyColumn on `Key was already used`. The fix keeps items distinct while the paging
        // cursor still advances by what the server sent, so the next window is correct.
        val firstPage = SearchResponse(
            products = listOf(
                Product(productId = "R1", productName = "One"),
                Product(productId = "R2", productName = "Two"),
                Product(productId = "PROMO", productName = "Promoted"),
            ),
            total = 5,
        )
        val secondPage = SearchResponse(
            products = listOf(
                Product(productId = "PROMO", productName = "Promoted"), // dup with page 1
                Product(productId = "R3", productName = "Three"),
                Product(productId = "R4", productName = "Four"),
            ),
            total = 5,
        )
        val api = api(search = Result.success(firstPage))
        val vm = viewModel(api = api)
        advanceUntilIdle()

        api.searchResult = Result.success(secondPage)
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(listOf("R1", "R2", "PROMO", "R3", "R4"), state.items.map { it.id })
        assertEquals(5, state.items.size)
        // loadedCount counts what the server sent (3 + 3), not what we displayed (5).
        assertEquals(6, state.loadedCount)
        // The next LoadMore, if it fired, would ask the server at offset 6.
        assertEquals(3, api.lastSearchRequest?.start)
    }

    @Test
    fun loadMoreDoesNothingWhenEveryItemIsAlreadyLoaded() = runTest {
        val api = api(search = Result.success(page(startId = 0, size = 5, total = 5)))
        val vm = viewModel(api = api)
        advanceUntilIdle()
        assertEquals(1, api.searchCallCount)

        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        // Guard held: nothing more to fetch, so no extra call.
        assertEquals(1, api.searchCallCount)
        assertEquals(5, vm.state.value.items.size)
    }

    @Test
    fun loadMoreIsIgnoredWhileAnotherFetchIsInFlight() = runTest {
        val api = api(search = Result.success(page(0, 20, 200)))
        val vm = viewModel(api = api)
        advanceUntilIdle()
        val callsAfterInitial = api.searchCallCount

        // Hold re-arms the gate, so the next request suspends until release.
        api.hold()
        api.searchResult = Result.success(page(20, 20, 200))
        vm.onIntent(ProductListIntent.LoadMore)
        runCurrent()
        assertTrue(vm.state.value.isLoading)
        val callsWithFirstLoadMore = api.searchCallCount
        assertEquals(callsAfterInitial + 1, callsWithFirstLoadMore)

        // Second LoadMore while the first is still parked on the gate: dropped by the guard.
        vm.onIntent(ProductListIntent.LoadMore)
        runCurrent()
        assertEquals(callsWithFirstLoadMore, api.searchCallCount)

        api.release()
        advanceUntilIdle()
        assertFalse(vm.state.value.isLoading)
        assertEquals(40, vm.state.value.items.size)
    }

    @Test
    fun loadMoreFailureSurfacesInErrorAndKeepsExistingItems() = runTest {
        val api = api(search = Result.success(page(0, 20, 200)))
        val vm = viewModel(api = api)
        advanceUntilIdle()

        api.searchResult = Result.failure(RuntimeException("boom"))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(ErrorState.UnknownErrorState, state.error)
        // The already-loaded list stays — a failed page must not blow it away. The composable
        // splits "full-page error" from "footer error" on items.isEmpty(), so a non-empty
        // list with an error renders as the footer variant.
        assertEquals(20, state.items.size)
    }

    @Test
    fun retryingAfterAPagingFailureClearsTheErrorAndAppends() = runTest {
        val api = api(search = Result.success(page(0, 20, 200)))
        val vm = viewModel(api = api)
        advanceUntilIdle()

        api.searchResult = Result.failure(RuntimeException("boom"))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(ErrorState.UnknownErrorState, vm.state.value.error)

        api.searchResult = Result.success(page(startId = 20, size = 20, total = 200))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        val state = vm.state.value
        assertNull(state.error)
        assertEquals(40, state.items.size)
    }

    @Test
    fun refreshResetsThePagingWindowAndClearsTheError() = runTest {
        // Load two pages, hit a paging error, then Refresh.
        val api = api(search = Result.success(page(0, 20, 200)))
        val vm = viewModel(api = api)
        advanceUntilIdle()

        api.searchResult = Result.success(page(20, 20, 200))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(40, vm.state.value.items.size)

        api.searchResult = Result.failure(RuntimeException("boom"))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()
        assertEquals(ErrorState.UnknownErrorState, vm.state.value.error)

        api.searchResult = Result.success(page(0, 20, 200))
        vm.onIntent(ProductListIntent.Refresh)
        advanceUntilIdle()

        val state = vm.state.value
        assertEquals(20, state.items.size)
        assertEquals(0, api.lastSearchRequest?.start)
        assertNull(state.error)
    }

    @Test
    fun cardTapEmitsOpenProductDetailsForTheTappedId() = runTest {
        // The screen turns the effect into a NavController.navigate — the ViewModel just has
        // to hand the id back out. No state changes, no repository calls.
        val vm = viewModel()
        advanceUntilIdle()

        vm.onIntent(ProductListIntent.OnCardClicked("R42"))

        assertEquals(ProductListEffect.OpenProductDetails("R42"), vm.effects.first())
    }

    @Test
    fun canLoadMoreReflectsWhatIsPossible() = runTest {
        val api = api(search = Result.success(page(0, 20, 40)))
        val vm = viewModel(api = api)
        advanceUntilIdle()
        assertTrue(vm.state.value.canLoadMore)

        api.searchResult = Result.success(page(20, 20, 40))
        vm.onIntent(ProductListIntent.LoadMore)
        advanceUntilIdle()

        // Reached the ceiling — the composable's auto-trigger stops firing.
        assertFalse(vm.state.value.canLoadMore)
    }
}
