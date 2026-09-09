package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * One filter the result set can be narrowed by — "Brand", "Colour", "Price" — with the values
 * it offers.
 *
 * [totalCount] is how many values exist, which is not always `values.size`: the response caps
 * long lists, and the difference is what a "show all" control would ask for.
 */
@Serializable
data class Facet(
    /** The query key, e.g. `c_color` or `price`. [name] is the label to show instead. */
    val id: String = "",
    val name: String = "",
    val totalCount: Int = 0,
    val values: List<FacetValue> = emptyList(),
)
