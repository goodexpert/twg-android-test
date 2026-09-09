package nz.co.warehouseandroidtest.kmp.session

import nz.co.warehouseandroidtest.kmp.data.User

/**
 * What the app persists about the signed-in party, kept deliberately separate from the
 * [User] wire type so stored data does not track the API's response shape.
 *
 * Named for a session rather than a guest: today [customerId] comes from the guest login,
 * but this is where a real token goes once one exists.
 */
data class UserSession(
    val customerId: String,
    val expiresAtEpochMillis: Long,
) {
    fun isValidAt(nowEpochMillis: Long): Boolean = nowEpochMillis < expiresAtEpochMillis
}

/**
 * Builds a session from a login response, or null when the response carries no customer id.
 *
 * Expiry is derived from [User.expiryMinutes] rather than [User.expiresDatetime]: the latter
 * is a string whose format has not been confirmed against a real response, and misparsing it
 * would either expire the session immediately or never.
 */
fun User.toSession(nowEpochMillis: Long): UserSession? {
    val id = customerId ?: return null
    return UserSession(
        customerId = id,
        expiresAtEpochMillis = nowEpochMillis + expiryMinutes * 60_000L,
    )
}
