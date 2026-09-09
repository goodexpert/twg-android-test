package nz.co.warehouseandroidtest.kmp

import android.os.Build

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val deviceHeader: String = "Android"
}

actual fun currentPlatform(): Platform = AndroidPlatform()
