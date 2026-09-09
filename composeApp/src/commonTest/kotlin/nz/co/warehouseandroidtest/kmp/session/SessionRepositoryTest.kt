package nz.co.warehouseandroidtest.kmp.session

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.kmp.data.User
import nz.co.warehouseandroidtest.kmp.network.WarehouseApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionRepositoryTest {

    private val now = 1_000_000L
    private val loggedInUser = User(customerId = "cust-123", expiryMinutes = 30)

    private fun repository(api: WarehouseApi, store: SessionStore) =
        SessionRepository(api, store, now = { now })

    @Test
    fun returnsStoredSessionWithoutCallingTheApi() = runTest {
        val api = FakeWarehouseApi(Result.success(loggedInUser))
        val store = SessionStore(MapSettings())
        store.write(UserSession("stored-id", expiresAtEpochMillis = now + 60_000))

        val session = repository(api, store).ensureSession().getOrThrow()

        assertEquals("stored-id", session.customerId)
        assertEquals(0, api.callCount)
    }

    @Test
    fun logsInWhenNothingIsStored() = runTest {
        val api = FakeWarehouseApi(Result.success(loggedInUser))
        val store = SessionStore(MapSettings())

        val session = repository(api, store).ensureSession().getOrThrow()

        assertEquals("cust-123", session.customerId)
        assertEquals(now + 30 * 60_000L, session.expiresAtEpochMillis)
        assertEquals(1, api.callCount)
        assertEquals(session, store.read())
    }

    @Test
    fun logsInAgainWhenTheStoredSessionExpired() = runTest {
        val api = FakeWarehouseApi(Result.success(loggedInUser))
        val store = SessionStore(MapSettings())
        store.write(UserSession("expired-id", expiresAtEpochMillis = now))

        val session = repository(api, store).ensureSession().getOrThrow()

        // The legacy app kept sending "expired-id" forever; this is the behaviour it lacked.
        assertEquals("cust-123", session.customerId)
        assertEquals(1, api.callCount)
        assertEquals("cust-123", store.read()?.customerId)
    }

    @Test
    fun propagatesLoginFailureAndLeavesTheStoreAlone() = runTest {
        val api = FakeWarehouseApi(Result.failure(RuntimeException("boom")))
        val store = SessionStore(MapSettings())

        val result = repository(api, store).ensureSession()

        assertTrue(result.isFailure)
        assertNull(store.read())
    }

    @OptIn(ExperimentalCoroutinesApi::class) // runCurrent
    @Test
    fun concurrentCallersTriggerASingleLogin() = runTest {
        // Uncompleted gate: loginAsGuest suspends, so callers stack up behind the first.
        val api = FakeWarehouseApi(Result.success(loggedInUser), gate = CompletableDeferred())
        val store = SessionStore(MapSettings())
        val repository = repository(api, store)

        val callers = List(5) { async { repository.ensureSession() } }
        // Let all five reach a suspension point: one inside loginAsGuest, four on the mutex.
        runCurrent()

        assertEquals(1, api.callCount)

        api.release()
        val results = callers.awaitAll()

        // The four that waited find the session the first one wrote, rather than logging in.
        assertEquals(1, api.callCount)
        assertTrue(results.all { it.isSuccess })
        assertTrue(results.all { it.getOrThrow().customerId == "cust-123" })
    }

    @Test
    fun failsWhenTheResponseHasNoCustomerId() = runTest {
        val api = FakeWarehouseApi(Result.success(User(customerId = null, expiryMinutes = 30)))
        val store = SessionStore(MapSettings())

        val result = repository(api, store).ensureSession()

        assertTrue(result.isFailure)
        assertNull(store.read())
    }
}
