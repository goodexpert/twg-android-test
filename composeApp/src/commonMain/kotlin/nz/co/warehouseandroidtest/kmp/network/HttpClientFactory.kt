package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import nz.co.warehouseandroidtest.kmp.currentPlatform

/**
 * Builds the client every TWG call goes through. Replaces the Retrofit + OkHttp setup in
 * `nz.co.warehouseandroidtest.WarehouseTestApp`.
 *
 * [engine] exists so tests can pass `MockEngine`. Left null, Ktor picks the single engine on
 * the platform classpath — OkHttp on Android, Darwin on iOS.
 */
fun createWarehouseHttpClient(engine: HttpClientEngine? = null): HttpClient {
    val configure: HttpClientConfig<*>.() -> Unit = {
        // Ktor does not treat a non-2xx as an error by default; without this a 401 would
        // deserialize into a failed parse rather than a clear transport failure.
        expectSuccess = true

        install(ContentNegotiation) {
            json(WarehouseJson)
            // The endpoints are static .json files behind APIM and their Content-Type has not
            // been confirmed. Registering text/plain too means a mislabelled response still
            // parses rather than failing with "no transformation found".
            json(WarehouseJson, ContentType.Text.Plain)
        }

        defaultRequest {
            url(WAREHOUSE_BASE_URL)
            header(HEADER_SUBSCRIPTION_KEY, SUBSCRIPTION_KEY)
            header(HEADER_DEVICE, currentPlatform().deviceHeader)
        }
    }

    return if (engine != null) HttpClient(engine, configure) else HttpClient(configure)
}

internal const val HEADER_SUBSCRIPTION_KEY: String = "Ocp-Apim-Subscription-Key"
internal const val HEADER_DEVICE: String = "X-TWL-Device"
