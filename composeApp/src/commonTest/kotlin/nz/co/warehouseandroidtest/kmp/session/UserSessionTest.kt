package nz.co.warehouseandroidtest.kmp.session

import nz.co.warehouseandroidtest.kmp.data.User
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class UserSessionTest {

    private val now = 1_000_000L

    @Test
    fun buildsSessionFromLoginResponse() {
        val user = User(customerId = "abc123", expiryMinutes = 30)

        val session = user.toSession(now)

        assertEquals("abc123", session?.customerId)
        assertEquals(now + 30 * 60_000L, session?.expiresAtEpochMillis)
    }

    @Test
    fun returnsNullWhenResponseHasNoCustomerId() {
        assertNull(User(customerId = null, expiryMinutes = 30).toSession(now))
    }

    @Test
    fun zeroExpiryMinutesYieldsAnAlreadyExpiredSession() {
        val session = User(customerId = "abc123", expiryMinutes = 0).toSession(now)

        assertEquals(false, session?.isValidAt(now))
    }
}
