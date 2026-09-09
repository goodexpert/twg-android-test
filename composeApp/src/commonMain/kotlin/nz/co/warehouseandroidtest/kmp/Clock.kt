package nz.co.warehouseandroidtest.kmp

/**
 * Wall-clock milliseconds since the epoch.
 *
 * An `expect`/`actual` rather than kotlinx-datetime: session expiry is the only thing that
 * needs the time, and one function is cheaper than a dependency. Callers take it as a
 * parameter so tests stay deterministic.
 */
expect fun currentTimeMillis(): Long
