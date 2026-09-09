package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * An ordering the result set can be asked for. The server sends the list, so the app does not
 * hardcode a menu that can fall out of step with what the endpoint accepts.
 */
@Serializable
data class SortOption(
    /** Sent back on a sorted request, e.g. `price-low-to-high`. */
    val id: String = "",
    val name: String = "",
)
