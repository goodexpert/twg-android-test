package nz.co.warehouseandroidtest.kmp.feature.productdetails

import nz.co.warehouseandroidtest.kmp.data.ErrorState
import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * One product, identified by what the scan decoded.
 *
 * [error] is state rather than a one-shot effect, for the same reason as the home screen's
 * login failure: a failed load stays set until it is retried, and the screen keeps its message
 * up for exactly that long. The legacy screen showed a Toast and left an empty layout behind
 * it.
 *
 * It carries the mapped [ErrorState] rather than a boolean, so the screen can say which of
 * "no connection", "server down" and "no such product" happened instead of blaming them all on
 * the connection.
 */
data class ProductDetailsUiState(
    val productId: String,
    val isLoading: Boolean = true,
    val product: ProductDetailsData? = null,
    val error: ErrorState? = null,
    /**
     * Which image the gallery is showing. Held here rather than in the composable because the
     * hero pager and the thumbnail row are two views of one selection, and a single owner is
     * what keeps them from disagreeing.
     */
    val selectedImageIndex: Int = 0,
) : UiState {

    val imageUrls: List<String> get() = product?.imageUrls.orEmpty()
}

sealed interface ProductDetailsIntent : UiIntent {
    data object Retry : ProductDetailsIntent

    /**
     * Sent both by a thumbnail tap and by a swipe of the hero image — they are the same action
     * as far as the state is concerned, so they share an intent.
     */
    data class ImageSelected(val index: Int) : ProductDetailsIntent
}
