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
    gate: CompletableDeferred<Unit> = CompletableDeferred(Unit),
) : WarehouseApi {

    /**
     * Reassignable so a test can let the first login finish and still catch the next one
     * mid-flight — see [hold]. A single immutable gate can only ever be opened once.
     */
    private var gate: CompletableDeferred<Unit> = gate

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

    /** Re-arms the gate, so the next [loginAsGuest] suspends until [release]. */
    fun hold() {
        gate = CompletableDeferred()
    }
}
