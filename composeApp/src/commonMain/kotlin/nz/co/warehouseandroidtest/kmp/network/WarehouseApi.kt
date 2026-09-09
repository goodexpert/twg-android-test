package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import nz.co.warehouseandroidtest.kmp.data.User

/**
 * The TWG endpoints. Ports `nz.co.warehouseandroidtest.WarehouseService`; only the login call
 * is implemented so far.
 */
interface WarehouseApi {
    suspend fun loginAsGuest(): Result<User>
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

    private companion object {
        const val LOGIN_PATH = "twgCSharpTest/Login.json"
        const val HEADER_AUTHORIZATION = "Authorization"
        const val GUEST = "Guest"
    }
}
