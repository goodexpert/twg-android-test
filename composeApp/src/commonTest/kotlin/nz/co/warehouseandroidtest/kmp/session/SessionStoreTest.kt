package nz.co.warehouseandroidtest.kmp.session

import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SessionStoreTest {

    private val now = 1_000_000L

    @Test
    fun readsBackWhatItWrote() {
        val store = SessionStore(MapSettings())
        val session = UserSession(customerId = "abc123", expiresAtEpochMillis = now + 60_000)

        store.write(session)

        assertEquals(session, store.read())
    }

    @Test
    fun readsNullWhenNothingStored() {
        assertNull(SessionStore(MapSettings()).read())
    }

    @Test
    fun clearRemovesTheSession() {
        val store = SessionStore(MapSettings())
        store.write(UserSession("abc123", now + 60_000))

        store.clear()

        assertNull(store.read())
    }

    @Test
    fun readIfValidReturnsSessionBeforeExpiry() {
        val store = SessionStore(MapSettings())
        store.write(UserSession("abc123", expiresAtEpochMillis = now + 1))

        assertEquals("abc123", store.readIfValid(now)?.customerId)
    }

    @Test
    fun readIfValidReturnsNullOnceExpired() {
        val store = SessionStore(MapSettings())
        store.write(UserSession("abc123", expiresAtEpochMillis = now))

        // The legacy app kept using the stored id forever; this is the case it never handled.
        assertNull(store.readIfValid(now))
    }
}
