package nz.co.warehouseandroidtest.kmp

/**
 * Platform facts the shared code needs.
 *
 * [deviceHeader] is the value the TWG APIM endpoints expect in `X-TWL-Device`; the legacy
 * Android app hardcodes "Android" in its OkHttp interceptor, which cannot survive the port.
 */
interface Platform {
    val name: String
    val deviceHeader: String
}

expect fun currentPlatform(): Platform
