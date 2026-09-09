package nz.co.warehouseandroidtest.kmp.search

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.kmp.data.Product
import nz.co.warehouseandroidtest.kmp.data.SearchResponse
import nz.co.warehouseandroidtest.kmp.data.User
import nz.co.warehouseandroidtest.kmp.network.FakeWarehouseApi
import nz.co.warehouseandroidtest.kmp.session.SessionRepository
import nz.co.warehouseandroidtest.kmp.session.SessionStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SearchRepositoryTest {

    private val loggedInUser = User(customerId = "cust-123", expiryMinutes = 30)

    private val results = SearchResponse(
        products = listOf(Product(productId = "R2436546", productName = "Copy Paper")),
        searchTerm = "Paper",
        total = 4276,
    )

    private fun repository(
        api: FakeWarehouseApi,
    ): SearchRepository = SearchRepository(
        api = api,
        sessionRepository = SessionRepository(api, SessionStore(MapSettings()), now = { 1_000L }),
    )

    private fun api(
        search: Result<SearchResponse> = Result.success(results),
        login: Result<User> = Result.success(loggedInUser),
    ) = FakeWarehouseApi(loginResult = login, searchResult = search)

    @Test
    fun returnsTheResultsForATerm() = runTest {
        val repository = repository(api())

        val response = repository.search("Paper").getOrThrow()

        assertEquals(4276, response.total)
        assertEquals("R2436546", response.products.single().productId)
    }

    @Test
    fun sendsTheSessionsCustomerIdAsTheUserId() = runTest {
        // The whole reason this repository takes a session: the endpoint requires a UserID.
        val api = api()

        repository(api).search("Paper")

        assertEquals("cust-123", api.lastSearchRequest?.userId)
        assertEquals(1, api.loginCallCount)
    }

    @Test
    fun passesThePagingWindowThrough() = runTest {
        val api = api()

        repository(api).search("Paper", start = 40, limit = 20)

        val request = api.lastSearchRequest
        assertEquals("Paper", request?.query)
        assertEquals(40, request?.start)
        assertEquals(20, request?.limit)
    }

    @Test
    fun defaultsToTheFirstPage() = runTest {
        val api = api()

        repository(api).search("Paper")

        assertEquals(0, api.lastSearchRequest?.start)
        assertEquals(SEARCH_PAGE_SIZE, api.lastSearchRequest?.limit)
    }

    @Test
    fun trimsTheTermBeforeSearching() = runTest {
        val api = api()

        repository(api).search("  Paper\n")

        assertEquals("Paper", api.lastSearchRequest?.query)
    }

    @Test
    fun failsOnABlankTermWithoutCallingTheApi() = runTest {
        // The endpoint would answer a blank term with the whole catalogue.
        val api = api()

        assertTrue(repository(api).search("   ").isFailure)
        assertEquals(0, api.searchCallCount)
    }

    @Test
    fun failsWhenThereIsNoSession() = runTest {
        // Better than sending a blank UserID, which is what the legacy app did when its login
        // had not come back yet.
        val api = api(login = Result.failure(RuntimeException("boom")))

        assertTrue(repository(api).search("Paper").isFailure)
        assertEquals(0, api.searchCallCount)
    }

    @Test
    fun propagatesATransportFailure() = runTest {
        val api = api(search = Result.failure(RuntimeException("boom")))

        assertTrue(repository(api).search("Paper").isFailure)
    }

    @Test
    fun rejectsANonsensicalPagingWindow() = runTest {
        // A caller error rather than a server one, so it fails loudly instead of becoming a
        // request the endpoint would answer strangely.
        val repository = repository(api())

        assertFailsWith<IllegalArgumentException> { repository.search("Paper", start = -1) }
        assertFailsWith<IllegalArgumentException> { repository.search("Paper", limit = 0) }
    }
}
