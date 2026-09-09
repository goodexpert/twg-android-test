package nz.co.warehouseandroidtest.kmp.session

import kotlinx.coroutines.CompletableDeferred
import nz.co.warehouseandroidtest.kmp.data.User
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi

/**
 * Records how often it was called so tests can assert the store short-circuits the network.
 *
 * By default [loginAsGuest] returns immediately. Pass an uncompleted [gate] to make it suspend
 * instead, which is what lets a test pile several callers up against one in-flight login; call
 * [release] to let them through.
 */
class FakeWarehouseApi(
    private val result: Result<User>,
    private val gate: CompletableDeferred<Unit> = CompletableDeferred(Unit),
) : WarehouseApi {

    var callCount: Int = 0
        private set

    override suspend fun loginAsGuest(): Result<User> {
        callCount++
        gate.await()
        return result
    }

    fun release() {
        gate.complete(Unit)
    }
}
