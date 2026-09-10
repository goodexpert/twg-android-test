package nz.co.warehouseandroidtest.kmp.feature.productdetails

import nz.co.warehouseandroidtest.kmp.data.Product

/**
 * One product as the details screen renders it.
 *
 * Separate from [nz.co.warehouseandroidtest.kmp.data.Product] because the wire type carries a
 * lot the screen does not, and because the "Clearance"/"Special" [badge] and the formatted
 * [price] are derived from the wire fields rather than sent as their own. The ViewModel maps
 * `Product` into this shape once and the composable never touches the wire type.
 */
data class ProductDetailsData(
    val id: String,
    val name: String,
    val brand: String? = null,
    val price: String? = null,
    val description: String? = null,
    val imageUrls: List<String> = emptyList(),
    val badge: String? = null,
    val isAvailable: Boolean = false,
    val barcode: String? = null,
    val features: List<String> = emptyList(),
)

internal fun Product.toProductDetailsData() = ProductDetailsData(
    id = productId,
    name = productName,
    brand = brand,
    price = formattedPrice,
    description = description,
    imageUrls = images,
    badge = when {
        isClearance -> "Clearance"
        onSpecial -> "Special"
        else -> null
    },
    isAvailable = isAvailable,
    barcode = barcode,
    features = featureList,
)
