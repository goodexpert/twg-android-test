package nz.co.warehouseandroidtest.kmp.search

import nz.co.warehouseandroidtest.kmp.data.SearchResponse
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi
import nz.co.warehouseandroidtest.kmp.session.SessionRepository

/** What the legacy `SearchResultActivity` asked for per page, and what a screen gets by default. */
const val SEARCH_PAGE_SIZE: Int = 20

/**
 * Runs one page of a search.
 *
 * Unlike [nz.co.warehouseandroidtest.kmp.product.ProductRepository], this one needs a session:
 * `Search.json` takes a UserID, so the login has to have happened. Asking for it here rather
 * than at the screen is what makes that guarantee hold for every caller — `ensureSession`
 * returns the stored session when it is still valid, so this is a request, not a login per
 * search.
 *
 * Paging is the caller's to drive: it passes [start], and [SearchResponse.total] says how far
 * it can go. The repository stays stateless so two screens searching at once cannot tread on
 * each other's offset.
 */
class SearchRepository(
    private val api: WarehouseApi,
    private val sessionRepository: SessionRepository,
) {

    suspend fun search(
        query: String,
        start: Int = 0,
        limit: Int = SEARCH_PAGE_SIZE,
    ): Result<SearchResponse> {
        val term = query.trim()
        // A blank term is a screen bug, not a search: the endpoint would answer with the whole
        // catalogue, one page at a time.
        if (term.isEmpty()) {
            return Result.failure(IllegalArgumentException("query is blank"))
        }
        require(start >= 0) { "start must not be negative, was $start" }
        require(limit > 0) { "limit must be positive, was $limit" }

        // Fails the search rather than sending a blank UserID, which is what the legacy app did
        // when its login had not returned yet.
        val session = sessionRepository.ensureSession().getOrElse { return Result.failure(it) }

        return api.getSearchResult(
            query = term,
            start = start,
            limit = limit,
            userId = session.customerId,
        )
    }
}
