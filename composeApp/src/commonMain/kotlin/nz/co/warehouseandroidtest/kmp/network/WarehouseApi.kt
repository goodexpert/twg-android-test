package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import nz.co.warehouseandroidtest.kmp.data.ProductResponse
import nz.co.warehouseandroidtest.kmp.data.SearchResponse
import nz.co.warehouseandroidtest.kmp.data.User

/** Ports `nz.co.warehouseandroidtest.WarehouseService`. */
interface WarehouseApi {
    suspend fun loginAsGuest(): Result<User>

    /**
     * [productId] is the catalogue id — `R2436546`, the value a scan decodes — not the barcode
     * and not `productKey`.
     */
    suspend fun getProduct(productId: String): Result<ProductResponse>

    /**
     * One page of results for [query].
     *
     * [start] is an offset into the whole result set and [limit] the page size, which is how
     * the legacy endless scroll listener paged: `SearchResponse.total` says how far it can go.
     *
     * [userId] comes from the session. Unlike the product endpoint, this one takes it, so a
     * caller has to have logged in first.
     */
    suspend fun getSearchResult(
        query: String,
        start: Int,
        limit: Int,
        userId: String,
    ): Result<SearchResponse>
}

class KtorWarehouseApi(private val client: HttpClient) : WarehouseApi {

    /**
     * `Authorization: Guest` is set here rather than on the shared client on purpose — the
     * legacy interface notes that Search and Product authenticate on the subscription key
     * alone.
     */
    override suspend fun loginAsGuest(): Result<User> = runCatching {
        client.get(LOGIN_PATH) {
            header(HEADER_AUTHORIZATION, GUEST)
        }.body<User>()
    }

    /**
     * No `Authorization` header, and no UserID either: this endpoint authenticates on the
     * subscription key alone, as the legacy `WarehouseService` notes. The legacy call also sent
     * MachineID, UserID and Branch alongside a `BarCode`; the current endpoint takes the product
     * id on its own.
     */
    override suspend fun getProduct(productId: String): Result<ProductResponse> =
        runCatching {
            client.get(PRODUCT_PATH) {
                parameter(PARAM_PRODUCT_ID, productId)
            }.body<ProductResponse>()
        }

    /**
     * MachineID and Branch are the fixed values the legacy app sent with every search — see
     * `nz.co.warehouseandroidtest.Constants`. They are constants here for the same reason: the
     * app has no branch picker, so there is nothing to vary them by yet.
     */
    override suspend fun getSearchResult(
        query: String,
        start: Int,
        limit: Int,
        userId: String,
    ): Result<SearchResponse> = runCatching {
        client.get(SEARCH_PATH) {
            parameter(PARAM_SEARCH, query)
            parameter(PARAM_START, start)
            parameter(PARAM_LIMIT, limit)
            parameter(PARAM_MACHINE_ID, MACHINE_ID)
            parameter(PARAM_USER_ID, userId)
            parameter(PARAM_BRANCH, BRANCH_ID)
        }.body<SearchResponse>()
    }

    private companion object {
        const val LOGIN_PATH = "twgCSharpTest/Login.json"
        const val PRODUCT_PATH = "twgCSharpTest/Product.json"
        const val SEARCH_PATH = "twgCSharpTest/Search.json"

        const val PARAM_PRODUCT_ID = "ProductId"
        const val PARAM_SEARCH = "Search"
        const val PARAM_START = "Start"
        const val PARAM_LIMIT = "Limit"
        const val PARAM_MACHINE_ID = "MachineID"
        const val PARAM_USER_ID = "UserID"
        const val PARAM_BRANCH = "Branch"

        const val MACHINE_ID = "1234567890"
        const val BRANCH_ID = "208"

        const val HEADER_AUTHORIZATION = "Authorization"
        const val GUEST = "Guest"
    }
}
