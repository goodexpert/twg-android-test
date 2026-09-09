package nz.co.warehouseandroidtest.kmp.network

/**
 * Whether this is "the request never got an answer" — no connection, unresolved host, timeout.
 *
 * Platform-specific because the engines are: OkHttp raises `java.io.IOException` subclasses,
 * while the Darwin engine wraps an `NSError`. There is no common supertype covering both, so
 * the check cannot live in common code.
 */
internal expect fun Throwable.isNetworkFailure(): Boolean
