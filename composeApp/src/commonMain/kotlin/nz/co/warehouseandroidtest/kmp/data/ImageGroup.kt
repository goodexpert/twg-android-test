package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * One set of a product's images, keyed by the variant they belong to.
 *
 * [colourAttribute] is empty for a product with no colour variants, which is the only case seen
 * so far; for one that has them it says which colour these images show.
 *
 * The URLs here are not always the same as [Product.imageUrls]: in the sample response the two
 * point at different hosts for the same picture, so they cannot be treated as interchangeable
 * without checking which one the app is meant to render.
 */
@Serializable
data class ImageGroup(
    val colourAttribute: String = "",
    val imageUrls: List<String> = emptyList(),
)
