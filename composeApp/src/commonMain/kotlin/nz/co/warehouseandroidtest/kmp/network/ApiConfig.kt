package nz.co.warehouseandroidtest.kmp.network

import kotlinx.serialization.json.Json

/** Ports `nz.co.warehouseandroidtest.Constants.HTTP_URL_ENDPOINT`. */
internal const val WAREHOUSE_BASE_URL: String = "https://legacy-apim.twg.co.nz/"

/**
 * `ignoreUnknownKeys` is not optional here: the responses carry fields the models do not
 * declare, and the strict default would fail the whole call over one of them.
 */
internal val WarehouseJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}
