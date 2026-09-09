package nz.co.warehouseandroidtest.kmp

import io.ktor.client.HttpClient
import nz.co.warehouseandroidtest.kmp.network.KtorWarehouseApi
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi
import nz.co.warehouseandroidtest.kmp.network.createWarehouseHttpClient
import nz.co.warehouseandroidtest.kmp.session.SessionRepository
import nz.co.warehouseandroidtest.kmp.session.SessionStore
import nz.co.warehouseandroidtest.kmp.session.createSecureSettings

/**
 * Holds everything whose lifetime is the whole process, and is the single place that knows how
 * those pieces fit together.
 *
 * This replaces the object-construction half of `nz.co.warehouseandroidtest.WarehouseTestApp`.
 * The other half of that class — handing Android a Context — lives in `WarehouseApplication`,
 * because it is the only part that cannot be platform-neutral.
 *
 * A class rather than an `object`, for two reasons. Tests can build one with fakes instead of
 * mutating global state, and screens can be handed the one dependency they need rather than
 * reaching in here for it — the difference between constructor injection and a service locator.
 *
 * Everything is `lazy`, which is what lets iOS skip initialisation entirely: nothing is built
 * until it is first read. It also means Android does not pay for EncryptedSharedPreferences'
 * master key at startup, only when something first touches the session.
 */
class AppContainer {

    /** Private on purpose: callers want an API, not a transport. */
    private val httpClient: HttpClient by lazy { createWarehouseHttpClient() }

    /** Private on purpose: [sessionRepository] is the supported way in. */
    private val sessionStore: SessionStore by lazy { SessionStore(createSecureSettings()) }

    val warehouseApi: WarehouseApi by lazy { KtorWarehouseApi(httpClient) }

    val sessionRepository: SessionRepository by lazy {
        SessionRepository(warehouseApi, sessionStore)
    }
}
