package nz.co.warehouseandroidtest.kmp.ui

import kotlinx.serialization.Serializable

/**
 * Destinations, as types rather than strings.
 *
 * Navigation 2.8 builds the route and its argument encoding from the serializer, so a
 * destination's arguments are checked by the compiler instead of parsed out of a path at
 * runtime. Deep links, when they arrive, derive their URI patterns from these same types.
 */
@Serializable
data object HomeRoute

@Serializable
data object QrScannerRoute

@Serializable
data object SearchRoute

/** [query] is what the search screen submitted, carried as part of the destination. */
@Serializable
data class ProductListRoute(val query: String)

/**
 * [productId] is whatever the scan decoded. The legacy app called the same value `barCode` and
 * sent it to `Product.json` as the `BarCode` query parameter, so the request builder will have
 * to map the name back when `getProductDetail` is ported.
 */
@Serializable
data class ProductDetailsRoute(val productId: String)
