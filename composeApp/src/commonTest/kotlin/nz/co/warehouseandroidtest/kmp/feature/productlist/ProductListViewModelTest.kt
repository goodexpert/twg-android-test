package nz.co.warehouseandroidtest.kmp.feature.productlist

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ProductListViewModelTest {

    @Test
    fun takesItsQueryFromTheRoute() {
        val viewModel = ProductListViewModel(query = "hammer")

        assertEquals("hammer", viewModel.state.value.query)
    }

    @Test
    fun startsIdle() {
        // Loading begins once getSearchResult is ported; nothing fetches yet.
        assertFalse(ProductListViewModel(query = "hammer").state.value.isLoading)
    }

    @Test
    fun refreshIsAcceptedWithoutChangingTheQuery() {
        val viewModel = ProductListViewModel(query = "hammer")

        viewModel.onIntent(ProductListIntent.Refresh)

        assertEquals("hammer", viewModel.state.value.query)
    }
}
