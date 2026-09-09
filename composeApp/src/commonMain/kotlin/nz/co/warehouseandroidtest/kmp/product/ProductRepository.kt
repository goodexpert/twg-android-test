package nz.co.warehouseandroidtest.kmp.product

import nz.co.warehouseandroidtest.kmp.data.Product
import nz.co.warehouseandroidtest.kmp.network.NotFoundException
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi

/**
 * Fetches one product and unwraps the envelope, so callers deal in [Product] rather than in the
 * shape of a response.
 *
 * No [nz.co.warehouseandroidtest.kmp.session.SessionRepository] here, unlike
 * [nz.co.warehouseandroidtest.kmp.search.SearchRepository]: `Product.json` authenticates on the
 * subscription key alone and takes no UserID. Adding the dependency anyway would let a failed
 * login break a screen that does not need one.
 */
class ProductRepository(private val api: WarehouseApi) {

    suspend fun product(productId: String): Result<Product> {
        val id = productId.trim()
        // Guarded here rather than at the endpoint: a blank id would fetch whatever the server
        // makes of an empty parameter, and that does not answer the question the caller asked.
        if (id.isEmpty()) {
            return Result.failure(IllegalArgumentException("productId is blank"))
        }

        return api.getProduct(id).mapCatching { response ->
            // A 200 with no product is how the endpoint reports an id it does not know, so it
            // is a failure for the caller even though the request succeeded. Thrown as
            // NotFoundException rather than a bare error() so it maps to the same ErrorState a
            // 404 would.
            response.product
                ?: throw NotFoundException("No product came back for id $id")
        }
    }
}
