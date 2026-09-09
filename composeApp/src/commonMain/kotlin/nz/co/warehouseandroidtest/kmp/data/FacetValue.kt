package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * One choice within a [Facet], with how many products it would leave.
 *
 * [id] is what a filtered request sends back; for a price band it is a range expression like
 * `(0..20)`, whose [name] reads "$0 – $19.99".
 */
@Serializable
data class FacetValue(
    val id: String = "",
    val name: String = "",
    val count: Int = 0,
)
