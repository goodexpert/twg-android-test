package nz.co.warehouseandroidtest.kmp.session

import com.russhwolf.settings.Settings

/**
 * Persists the current [UserSession]. Replaces `nz.co.warehouseandroidtest.Utils.PreferenceUtil`,
 * which stored a bare customer id in plain SharedPreferences and never refreshed it.
 *
 * Backed by the Keychain on iOS and EncryptedSharedPreferences on Android; see
 * [createSecureSettings].
 *
 * The two fields are stored as primitives rather than a serialized blob. Storage then carries
 * no format of its own to migrate, which matters more than saving one key.
 */
class SessionStore(private val settings: Settings) {

    fun read(): UserSession? {
        val customerId = settings.getStringOrNull(KEY_CUSTOMER_ID) ?: return null
        val expiresAt = settings.getLongOrNull(KEY_EXPIRES_AT) ?: return null
        return UserSession(customerId, expiresAt)
    }

    /**
     * The stored session if it has not expired, else null. Callers treat null as "log in
     * again" — the behaviour the legacy app was missing, since it only ever logged in when
     * nothing had been stored yet.
     */
    fun readIfValid(nowEpochMillis: Long): UserSession? =
        read()?.takeIf { it.isValidAt(nowEpochMillis) }

    fun write(session: UserSession) {
        settings.putString(KEY_CUSTOMER_ID, session.customerId)
        settings.putLong(KEY_EXPIRES_AT, session.expiresAtEpochMillis)
    }

    fun clear() {
        settings.remove(KEY_CUSTOMER_ID)
        settings.remove(KEY_EXPIRES_AT)
    }

    private companion object {
        const val KEY_CUSTOMER_ID = "customerId"
        const val KEY_EXPIRES_AT = "expiresAtEpochMillis"
    }
}

/**
 * A [Settings] backed by the platform's secure store.
 *
 * On Android this needs a Context, so [initSecureSettings] must run first. That wiring moves
 * into dependency injection once it lands.
 */
expect fun createSecureSettings(): Settings
