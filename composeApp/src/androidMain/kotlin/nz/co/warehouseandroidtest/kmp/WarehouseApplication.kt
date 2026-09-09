package nz.co.warehouseandroidtest.kmp

import android.app.Application
import nz.co.warehouseandroidtest.kmp.session.initSecureSettings

/**
 * Two jobs, both of which have to happen once per process.
 *
 * It hands Android's Context to the secure settings store — the one piece of [AppContainer]'s
 * graph that common code cannot build for itself — and it owns the container, so every screen
 * shares one HttpClient and one session rather than building its own.
 *
 * Application rather than an Activity because `MainActivity` is recreated on configuration
 * change and is not the only way a process can start. It is the split the legacy
 * `WarehouseTestApp` never made: the graph itself stays platform-neutral in [AppContainer],
 * and only the Context seam lives here.
 */
class WarehouseApplication : Application() {

    /**
     * Safe to build before [onCreate] runs: everything inside is `lazy`, so nothing reads the
     * secure store until a screen actually asks for a session.
     */
    val container: AppContainer = AppContainer()

    override fun onCreate() {
        super.onCreate()
        initSecureSettings(this)
    }
}
