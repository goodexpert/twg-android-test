package nz.co.warehouseandroidtest.kmp

import platform.UIKit.UIDevice

actual fun currentPlatform(): Platform = object : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val deviceHeader: String = "iOS"
}
