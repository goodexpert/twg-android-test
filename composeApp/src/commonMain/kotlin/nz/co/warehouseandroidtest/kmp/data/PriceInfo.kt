package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * A product's price.
 *
 * A wrapper rather than a bare number on [Product]: the endpoint nests the price and carries
 * promotion prices alongside it.
 */
@Serializable
data class PriceInfo(
    val price: Double? = null,
)
