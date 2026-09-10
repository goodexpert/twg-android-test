package nz.co.warehouseandroidtest.kmp.feature.productdetails

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nz.co.warehouseandroidtest.kmp.TestFixtures
import nz.co.warehouseandroidtest.kmp.data.ErrorState
import nz.co.warehouseandroidtest.kmp.data.ProductResponse
import nz.co.warehouseandroidtest.kmp.network.FakeWarehouseApi
import nz.co.warehouseandroidtest.kmp.network.WarehouseJson
import nz.co.warehouseandroidtest.kmp.product.ProductRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * `viewModelScope` runs on `Dispatchers.Main`, which is not the scheduler `runTest` drives.
 * Without [setMain] the load in `init` is dispatched somewhere the test cannot advance.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailsViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    /**
     * Parsed once from the same fixture the network test decodes, so these tests bind to the
     * shape the endpoint actually returns rather than to a hand-crafted stub. `WarehouseJson`
     * is the same parser the app runs in production.
     */
    private val productResponse: ProductResponse =
        WarehouseJson.decodeFromString(TestFixtures.productJson)

    /**
     * Three images, so a selection has somewhere to move to. The fixture ships with a single
     * image, so we copy over it — everything else about the product stays as the wire sent it.
     */
    private val galleryResponse: ProductResponse = productResponse.copy(
        product = productResponse.product?.copy(
            imageUrls = listOf(
                "https://example.test/1.jpg",
                "https://example.test/2.jpg",
                "https://example.test/3.jpg",
            ),
        ),
    )

    @BeforeTest
    fun substituteMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun viewModel(api: FakeWarehouseApi, productId: String = "R2436546") =
        ProductDetailsViewModel(productId, ProductRepository(api))

    @Test
    fun takesItsProductIdFromTheRoute() {
        val api = FakeWarehouseApi(productResult = Result.success(productResponse))

        assertEquals("R2436546", viewModel(api).state.value.productId)
    }

    @Test
    fun loadsTheProductAsSoonAsTheScreenOpens() = runTest {
        // Held, so the request is still in flight when the assertions run.
        val api = FakeWarehouseApi(
            productResult = Result.success(productResponse),
            gate = CompletableDeferred(),
        )

        val viewModel = viewModel(api)
        runCurrent()

        assertEquals(1, api.productCallCount)
        assertEquals("R2436546", api.lastProductId)
        // The spinner has something to show while the request is outstanding.
        assertTrue(viewModel.state.value.isLoading)

        api.release()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun publishesTheProductOnceItArrives() = runTest {
        val api = FakeWarehouseApi(productResult = Result.success(productResponse))

        val viewModel = viewModel(api)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
        assertEquals("$9.99", assertNotNull(state.product).price)
    }

    @Test
    fun reportsFailureWhenTheLoadFails() = runTest {
        val api = FakeWarehouseApi(productResult = Result.failure(RuntimeException("boom")))

        val viewModel = viewModel(api)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(ErrorState.UnknownErrorState, state.error)
        assertNull(state.product)
    }

    @Test
    fun tellsTheScreenWhenTheProductDoesNotExist() = runTest {
        // The state has to carry which failure it was: this one is not a connection problem,
        // and retrying it would ask the same question again.
        val api = FakeWarehouseApi(
            productResult = Result.success(productResponse.copy(product = null))
        )

        val viewModel = viewModel(api, productId = "R0000000")
        advanceUntilIdle()

        assertEquals(ErrorState.NotFoundErrorState, viewModel.state.value.error)
    }

    @Test
    fun opensOnTheFirstImage() = runTest {
        val api = FakeWarehouseApi(productResult = Result.success(productResponse))

        val viewModel = viewModel(api)
        advanceUntilIdle()

        assertEquals(0, viewModel.state.value.selectedImageIndex)
    }

    @Test
    fun selectsAnotherImage() = runTest {
        // Both a thumbnail tap and a settled swipe arrive as this intent.
        val api = FakeWarehouseApi(productResult = Result.success(galleryResponse))
        val viewModel = viewModel(api)
        advanceUntilIdle()

        viewModel.onIntent(ProductDetailsIntent.ImageSelected(2))

        assertEquals(2, viewModel.state.value.selectedImageIndex)
    }

    @Test
    fun ignoresAnIndexTheProductHasNoImageFor() = runTest {
        // The pager can settle while a reload swaps in a product with fewer images; the
        // selection must not end up pointing past the end of the list.
        val api = FakeWarehouseApi(productResult = Result.success(galleryResponse))
        val viewModel = viewModel(api)
        advanceUntilIdle()
        viewModel.onIntent(ProductDetailsIntent.ImageSelected(2))

        viewModel.onIntent(ProductDetailsIntent.ImageSelected(3))
        viewModel.onIntent(ProductDetailsIntent.ImageSelected(-1))

        assertEquals(2, viewModel.state.value.selectedImageIndex)
    }

    @Test
    fun retryRunsAnotherLoad() = runTest {
        val api = FakeWarehouseApi(productResult = Result.failure(RuntimeException("boom")))
        val viewModel = viewModel(api)
        advanceUntilIdle()
        assertEquals(ErrorState.UnknownErrorState, viewModel.state.value.error)

        api.productResult = Result.success(productResponse)
        viewModel.onIntent(ProductDetailsIntent.Retry)
        advanceUntilIdle()

        assertEquals(2, api.productCallCount)
        assertNull(viewModel.state.value.error)
        assertNotNull(viewModel.state.value.product)
    }

    @Test
    fun retryClearsTheFailureBeforeTryingAgain() = runTest {
        // Otherwise the screen would keep its old error up beside the spinner.
        val api = FakeWarehouseApi(productResult = Result.failure(RuntimeException("boom")))
        val viewModel = viewModel(api)
        advanceUntilIdle()

        api.hold()
        viewModel.onIntent(ProductDetailsIntent.Retry)
        runCurrent()

        assertNull(viewModel.state.value.error)
        assertTrue(viewModel.state.value.isLoading)

        // Let the retry finish, so the test does not leave a coroutine parked on the gate.
        api.release()
        advanceUntilIdle()
        assertEquals(ErrorState.UnknownErrorState, viewModel.state.value.error)
    }

    @Test
    fun keepsTheLoadedProductWhenARetryFails() = runTest {
        val api = FakeWarehouseApi(productResult = Result.success(productResponse))
        val viewModel = viewModel(api)
        advanceUntilIdle()
        val loaded = assertNotNull(viewModel.state.value.product)

        api.productResult = Result.failure(RuntimeException("boom"))
        viewModel.onIntent(ProductDetailsIntent.Retry)
        advanceUntilIdle()

        assertEquals(ErrorState.UnknownErrorState, viewModel.state.value.error)
        assertEquals(loaded, viewModel.state.value.product)
    }
}
