package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/** What [Product] reports about its own stock. */
@Serializable
data class Inventory(
    val available: Boolean = false,
    /**
     * Orderable before it exists, and orderable while out of stock. Both mean "you can buy
     * this, but not today", which is a different message from [available] being false.
     */
    val preorderable: Boolean = false,
    val backorderable: Boolean = false,
    /** Stock on hand. */
    val soh: Int = 0,
)
