package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * Where else the search could go: categories that match the term, and brands that do.
 *
 * [brands] is a list of names rather than of objects — the endpoint sends bare strings here,
 * unlike the brand facet, which carries counts.
 */
@Serializable
data class Suggestions(
    val categories: List<Category> = emptyList(),
    val brands: List<String> = emptyList(),
)
