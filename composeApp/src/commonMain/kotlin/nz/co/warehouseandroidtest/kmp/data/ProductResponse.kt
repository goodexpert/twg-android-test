package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * Response of `twgCSharpTest/Product.json`, the envelope around one [Product].
 *
 * This is the v4.9 shape, which is not the one `nz.co.warehouseandroidtest.data.ProductDetail`
 * models: the legacy type expects a flat PascalCase object keyed on a scanned `BarCode`, while
 * the endpoint now answers a camelCase `product` wrapped in an envelope and keyed on
 * `ProductId`. Nothing is carried over from that class as a result.
 *
 * [SearchResponse] wraps the same [Product] type, as a list.
 */
@Serializable
data class ProductResponse(
    /** Absent when the id matches nothing, so the screen cannot assume a product came back. */
    val product: Product? = null,
    val guest: Boolean = false,
    val apiVersion: Double = 0.0,
)
