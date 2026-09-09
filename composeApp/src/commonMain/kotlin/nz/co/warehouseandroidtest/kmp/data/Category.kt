package nz.co.warehouseandroidtest.kmp.data

import kotlinx.serialization.Serializable

/**
 * A node of the catalogue tree, as the search suggestions return it.
 *
 * The parent fields are absent on a top-level category rather than empty, which is how a caller
 * tells the root from the rest.
 */
@Serializable
data class Category(
    val categoryId: String = "",
    val parentCategoryId: String? = null,
    val parentCategoryName: String? = null,
    val name: String = "",
    val description: String? = null,
    /**
     * Null in every sample seen so far, so the type it takes when set is unconfirmed. String is
     * the assumption the other ids in this response support.
     */
    val sizeChartId: String? = null,
    /** How many products the category holds — what a suggestion row shows beside its name. */
    val productCount: Int = 0,
    val subCategoryCount: Int = 0,
    val showInBrowse: Boolean = false,
    val excludeFromVisualBrowse: Boolean = false,
)
