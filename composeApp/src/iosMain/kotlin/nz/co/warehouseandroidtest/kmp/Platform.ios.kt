package nz.co.warehouseandroidtest.kmp

import platform.UIKit.UIDevice

class IOSPlatform : Platform {
    override val name: String =
        UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
    override val deviceHeader: String = "iOS"
}

actual fun currentPlatform(): Platform = IOSPlatform()
