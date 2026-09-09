package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * One offer attached to a product.
 *
 * [price] is the price under this promotion, which is not always the price the product sells
 * at: a response can carry several promotions, and some of them — delivery offers, basket-level
 * discounts — repeat the ordinary price. [PriceInfo] stays the source for what the screen shows.
 *
 * [dealDescription] is the customer-facing line ("Get $8 off when you spend $80"); [description]
 * is the internal name for the same offer, and the two are often but not always the same text.
 */
@Serializable
data class Promotion(
    val promotionId: String = "",
    val dealDescription: String? = null,
    /** The small print under [dealDescription] — "Excludes OS Products". Not always sent. */
    val demandwareConditionsText: String? = null,
    val description: String? = null,
    val price: Double? = null,
    val isMarketClubExclusive: Boolean = false,
    val tags: List<String> = emptyList(),
)
