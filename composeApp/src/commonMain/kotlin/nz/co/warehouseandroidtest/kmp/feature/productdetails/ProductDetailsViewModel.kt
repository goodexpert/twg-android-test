package nz.co.warehouseandroidtest.kmp.feature.productdetails

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.kmp.network.toErrorState
import nz.co.warehouseandroidtest.kmp.product.ProductRepository
import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Takes the product id straight from the route, so the screen cannot exist without one, and
 * loads that product once on open.
 *
 * The load runs on `viewModelScope`, so it is cancelled with the screen rather than with the
 * composition, and a rotation does not start it again.
 *
 * `Nothing` as the effect type: this screen has no one-shot events, and that makes
 * `sendEffect` uncallable rather than merely unused.
 */
class ProductDetailsViewModel(
    productId: String,
    private val repository: ProductRepository,
) : BaseViewModel<ProductDetailsUiState, ProductDetailsIntent, Nothing>(
    ProductDetailsUiState(productId = productId),
) {

    init {
        load()
    }

    override fun handleIntent(intent: ProductDetailsIntent) {
        when (intent) {
            ProductDetailsIntent.Retry -> load()

            is ProductDetailsIntent.ImageSelected -> selectImage(intent.index)
        }
    }

    /**
     * Ignores an index the current product has no image for. A pager settling while a reload
     * swaps in a product with fewer images would otherwise leave the selection pointing past
     * the end of the list.
     */
    private fun selectImage(index: Int) {
        if (index !in currentState.imageUrls.indices) return

        setState { copy(selectedImageIndex = index) }
    }

    private fun load() {
        viewModelScope.launch {
            // Clearing the failure up front means a retry does not show the old error next to
            // the spinner while it is in flight.
            setState { copy(isLoading = true, error = null) }

            val result = repository.product(currentState.productId)
                .map { it.toProductDetailsData() }

            setState {
                // Kept on failure rather than cleared: a refresh that fails should leave what
                // is on screen alone.
                val loaded = result.getOrNull() ?: product

                copy(
                    isLoading = false,
                    product = loaded,
                    error = result.exceptionOrNull()?.toErrorState(),
                    // A new product means a new gallery. Back to its first image rather than
                    // to whatever position the previous one happened to be at.
                    selectedImageIndex = if (loaded !== product) 0 else selectedImageIndex,
                )
            }
        }
    }
}
