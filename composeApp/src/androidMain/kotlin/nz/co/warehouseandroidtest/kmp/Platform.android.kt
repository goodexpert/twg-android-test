package nz.co.warehouseandroidtest.kmp

import android.os.Build

actual fun currentPlatform(): Platform = object : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
    override val deviceHeader: String = "Android"
}
