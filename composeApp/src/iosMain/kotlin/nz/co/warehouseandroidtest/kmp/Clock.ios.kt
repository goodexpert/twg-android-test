package nz.co.warehouseandroidtest.kmp

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970

// NSDate reports the Unix epoch offset in seconds; the shared API works in milliseconds.
private const val MILLIS_PER_SECOND = 1_000

actual fun currentTimeMillis(): Long =
    (NSDate().timeIntervalSince1970 * MILLIS_PER_SECOND).toLong()
