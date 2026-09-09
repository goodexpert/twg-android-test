package nz.co.warehouseandroidtest.kmp.storage

import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings

private var applicationContext: Context? = null

/**
 * Must run before any store is built, from the Android entry point. Both the secure session
 * store and the ordinary settings here need a Context, so they share one holder rather than
 * each keeping their own.
 */
fun initPlatformStorage(context: Context) {
    applicationContext = context.applicationContext
}

internal fun requireApplicationContext(): Context = requireNotNull(applicationContext) {
    "initPlatformStorage(context) must be called before any store is created"
}

actual fun createSettings(name: String): Settings =
    SharedPreferencesSettings(
        requireApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE)
    )
