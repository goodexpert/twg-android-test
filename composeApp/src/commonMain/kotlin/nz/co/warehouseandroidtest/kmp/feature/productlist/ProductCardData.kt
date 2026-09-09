package nz.co.warehouseandroidtest.kmp.feature.productlist

import nz.co.warehouseandroidtest.kmp.data.Product
import nz.co.warehouseandroidtest.kmp.data.formatNzd

/**
 * One product as the card renders it.
 *
 * Separate from [nz.co.warehouseandroidtest.kmp.data.Product] because the wire type carries a
 * lot the card does not, and because "Was $5.95" and "SAVE $1.00" are derived from promotions
 * rather than sent as their own fields. Once the search endpoint is wired in, the ViewModel
 * maps `Product` into this shape once and the composable never touches the wire type.
 */
data class ProductCardData(
    val id: String,
    val name: String,
    val description: String? = null,
    val imageUrl: String? = null,
    val price: String,
    val unitPrice: String? = null,
    val wasPrice: String? = null,
    val savingLabel: String? = null,
    val discountLabel: String? = null,
    val isPromoted: Boolean = false,
    val isSpecial: Boolean = false,
)

internal fun Product.toProductCardData() =
    ProductCardData(
        id = productId,
        name = productName,
        description = productDescription,
        imageUrl = productImageUrl,
        price = formattedPrice ?: formatNzd(0.0),
        isSpecial = onSpecial,
    )