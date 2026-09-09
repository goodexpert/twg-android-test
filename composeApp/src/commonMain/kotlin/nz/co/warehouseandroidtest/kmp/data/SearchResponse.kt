package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * Response of `twgCSharpTest/Search.json`.
 *
 * [products] carries the same product shape `Product.json` returns, minus the fields only the
 * detail view needs — no `imageUrls`, `featureList` or `isClearance`. Every one of those has a
 * default on [Product], so one type serves both endpoints rather than a near-duplicate per
 * call.
 *
 * [total] is the size of the whole result set, not of [products]: the request pages through it
 * with `Start` and `Limit`, which is what the legacy endless scroll listener drove.
 */
@Serializable
data class SearchResponse(
    val products: List<Product> = emptyList(),
    /** What the server understood the query to be, which is not always what was typed. */
    val searchTerm: String = "",
    val suggestions: Suggestions? = null,
    val total: Int = 0,
    val facets: List<Facet> = emptyList(),
    val sortOptions: List<SortOption> = emptyList(),
    val guest: Boolean = false,
    val apiVersion: Double = 0.0,
)
