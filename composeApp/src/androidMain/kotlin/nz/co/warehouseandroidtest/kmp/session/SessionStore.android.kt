@file:Suppress("DEPRECATION")

package nz.co.warehouseandroidtest.kmp.session

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings

private var applicationContext: Context? = null

/**
 * Must be called before [createSecureSettings], from the Android entry point. A temporary
 * seam: it disappears when dependency injection supplies the Context instead.
 */
fun initSecureSettings(context: Context) {
    applicationContext = context.applicationContext
}

/**
 * EncryptedSharedPreferences and MasterKey are deprecated as of androidx.security-crypto
 * 1.1.0 — the version is stable, the API is not the way forward. They are kept because they
 * still work, the alternative is hand-rolling Android Keystore encryption, and the value at
 * stake today is a guest id rather than a credential.
 *
 * Revisit before this holds a real token: at that point the storage matters and picking a
 * supported replacement is worth the work.
 */
actual fun createSecureSettings(): Settings {
    val context = requireNotNull(applicationContext) {
        "initSecureSettings(context) must be called before createSecureSettings()"
    }
    val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    return SharedPreferencesSettings(preferences)
}

private const val PREFERENCES_NAME = "warehouse_session"
