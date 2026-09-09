package nz.co.warehouseandroidtest.kmp.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import nz.co.warehouseandroidtest.kmp.currentTimeMillis
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi

/**
 * Supplies a usable session, logging in as a guest when there isn't one.
 *
 * Replaces the logic in `nz.co.warehouseandroidtest.MainActivity.onResume`, which logged in
 * only when nothing had ever been stored. Because it never looked at the expiry the response
 * carries, a stored id was used forever; here an expired session triggers a fresh login.
 */
class SessionRepository(
    private val api: WarehouseApi,
    private val store: SessionStore,
    private val now: () -> Long = ::currentTimeMillis,
) {

    /**
     * Serialises logins so concurrent callers produce one, not one each. This is a coroutine
     * mutex: waiting suspends rather than blocking a thread.
     *
     * Against the current API — `Login.json` is a static file returning the same customer id
     * every time — duplicate logins would be wasted requests rather than wrong data. The guard
     * is here because this is where a real token will eventually be issued, and at that point
     * concurrent logins would hand different callers different sessions.
     */
    private val loginMutex = Mutex()

    suspend fun ensureSession(): Result<UserSession> {
        // Checked outside the lock: the common path has a valid session and should never
        // contend.
        store.readIfValid(now())?.let { return Result.success(it) }

        return loginMutex.withLock {
            // Checked again inside: whoever held the lock may have just logged in, in which
            // case this caller must not log in a second time.
            store.readIfValid(now())?.let { return@withLock Result.success(it) }

            api.loginAsGuest().mapCatching { user ->
                // A login that returns no customer id is a failure. The legacy code silently
                // did nothing here, leaving the app to send a null UserID on every later
                // request.
                val session = user.toSession(now())
                    ?: error("Login succeeded but the response carried no customerId")
                store.write(session)
                session
            }
        }
    }
}
