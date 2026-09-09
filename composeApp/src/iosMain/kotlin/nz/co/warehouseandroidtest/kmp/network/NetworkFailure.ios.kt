package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.engine.darwin.DarwinHttpRequestException
import kotlinx.io.IOException

/**
 * The Darwin engine wraps the underlying `NSError` in [DarwinHttpRequestException]; Ktor's own
 * timeout and socket failures arrive as kotlinx.io [IOException]. Both mean the request never
 * got an answer.
 */
internal actual fun Throwable.isNetworkFailure(): Boolean =
    this is DarwinHttpRequestException || this is IOException
