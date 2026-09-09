package nz.co.warehouseandroidtest.kmp.network

import java.io.IOException

/**
 * OkHttp reports every transport failure as an [IOException] — `UnknownHostException`,
 * `SocketTimeoutException`, `ConnectException` — and Ktor's own timeout exceptions extend it
 * too, so the one check covers them all.
 */
internal actual fun Throwable.isNetworkFailure(): Boolean = this is IOException
