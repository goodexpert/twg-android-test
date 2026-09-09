package nz.co.warehouseandroidtest.kmp.product

import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.kmp.data.Product
import nz.co.warehouseandroidtest.kmp.data.ProductResponse
import nz.co.warehouseandroidtest.kmp.network.FakeWarehouseApi
import nz.co.warehouseandroidtest.kmp.network.NotFoundException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ProductRepositoryTest {

    private val sample = Product(
        productId = "R2436546",
        productName = "Reflex Premium Copy Paper 80gsm 500 Sheet Ream A4",
    )

    /** Returns the fake alongside the repository, so a test can assert on both. */
    private fun repository(result: Result<ProductResponse>): Pair<FakeWarehouseApi, ProductRepository> {
        val api = FakeWarehouseApi(productResult = result)
        return api to ProductRepository(api)
    }

    @Test
    fun unwrapsTheProductFromTheEnvelope() = runTest {
        val (_, repository) = repository(Result.success(ProductResponse(product = sample)))

        assertEquals(sample, repository.product("R2436546").getOrThrow())
    }

    @Test
    fun trimsTheIdBeforeAskingTheApi() = runTest {
        // A decoded scan can carry a trailing newline; the query parameter must not.
        val (api, repository) = repository(Result.success(ProductResponse(product = sample)))

        repository.product(" R2436546\n")

        assertEquals("R2436546", api.lastProductId)
    }

    @Test
    fun failsOnABlankIdWithoutCallingTheApi() = runTest {
        val (api, repository) = repository(Result.success(ProductResponse(product = sample)))

        assertTrue(repository.product("  ").isFailure)
        assertEquals(0, api.productCallCount)
    }

    @Test
    fun reportsAnEmptyEnvelopeAsNotFound() = runTest {
        // How the endpoint answers an id it does not know: 200, no product. Reported as
        // NotFoundException so it maps to the same ErrorState a 404 would.
        val (_, repository) = repository(Result.success(ProductResponse(product = null)))

        val failure = repository.product("R0000000").exceptionOrNull()

        assertTrue(failure is NotFoundException, "was ${failure?.let { it::class.simpleName }}")
    }

    @Test
    fun propagatesATransportFailure() = runTest {
        val (_, repository) = repository(Result.failure(RuntimeException("boom")))

        assertTrue(repository.product("R2436546").isFailure)
    }

    @Test
    fun doesNotTouchTheSession() = runTest {
        // The endpoint takes no UserID. A login the fake never stubbed would fail loudly if
        // this repository asked for one.
        val (api, repository) = repository(Result.success(ProductResponse(product = sample)))

        assertTrue(repository.product("R2436546").isSuccess)
        assertEquals(0, api.loginCallCount)
    }
}
