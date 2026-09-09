package nz.co.warehouseandroidtest.kmp.network

import kotlinx.coroutines.CompletableDeferred
import nz.co.warehouseandroidtest.kmp.data.ProductResponse
import nz.co.warehouseandroidtest.kmp.data.SearchResponse
import nz.co.warehouseandroidtest.kmp.data.User

/**
 * One fake for the whole API, rather than one per endpoint: a second implementation of
 * [WarehouseApi] only exists to be updated every time the interface grows a method.
 *
 * A test stubs the calls it exercises and leaves the rest alone — an unstubbed call fails
 * loudly rather than returning something plausible. Every result is a `var`, so a test can fail
 * the first call and succeed on the retry.
 *
 * The call counters are separate because a test asserting "logged in once" should not be
 * disturbed by an unrelated product fetch.
 *
 * By default calls return immediately. Pass an uncompleted [gate], or call [hold], to make
 * them suspend instead — that is what lets a test pile callers up against one in-flight
 * request, or look at a screen while a load is still running; [release] lets them through. The
 * gate is shared by every call, which no test has needed to separate.
 */
class FakeWarehouseApi(
    var loginResult: Result<User> = notStubbed("loginAsGuest"),
    var productResult: Result<ProductResponse> = notStubbed("getProduct"),
    var searchResult: Result<SearchResponse> = notStubbed("getSearchResult"),
    gate: CompletableDeferred<Unit> = CompletableDeferred(Unit),
) : WarehouseApi {

    /**
     * Reassignable so a test can let the first call finish and still catch the next one
     * mid-flight — see [hold]. A single immutable gate can only ever be opened once.
     */
    private var gate: CompletableDeferred<Unit> = gate

    var loginCallCount: Int = 0
        private set

    var productCallCount: Int = 0
        private set

    var searchCallCount: Int = 0
        private set

    var lastProductId: String? = null
        private set

    /** The query, offset, page size and user id the last search was asked for. */
    var lastSearchRequest: SearchRequest? = null
        private set

    override suspend fun loginAsGuest(): Result<User> {
        loginCallCount++
        gate.await()
        return loginResult
    }

    override suspend fun getProduct(productId: String): Result<ProductResponse> {
        productCallCount++
        lastProductId = productId
        gate.await()
        return productResult
    }

    override suspend fun getSearchResult(
        query: String,
        start: Int,
        limit: Int,
        userId: String,
    ): Result<SearchResponse> {
        searchCallCount++
        lastSearchRequest = SearchRequest(query, start, limit, userId)
        gate.await()
        return searchResult
    }

    fun release() {
        gate.complete(Unit)
    }

    /** Re-arms the gate, so the next call suspends until [release]. */
    fun hold() {
        gate = CompletableDeferred()
    }
}

/** What a search was asked for, so a test can assert the paging arithmetic in one comparison. */
data class SearchRequest(
    val query: String,
    val start: Int,
    val limit: Int,
    val userId: String,
)

private fun <T> notStubbed(call: String): Result<T> =
    Result.failure(UnsupportedOperationException("$call is not stubbed on this fake"))
