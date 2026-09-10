package nz.co.warehouseandroidtest.kmp.deeplink

import io.ktor.http.decodeURLQueryComponent
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import nz.co.warehouseandroidtest.kmp.ui.ProductDetailsRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductListRoute

/**
 * URL scheme the platforms advertise for the app.
 *
 * Kept in one place so `AndroidManifest.xml`, `Info.plist`, and [parseDeepLink] cannot drift.
 * Both sides of the wire have to agree on the exact string.
 */
const val DEEP_LINK_SCHEME: String = "nz.co.thewarehousegroup.app.kmp"

private const val PRODUCT_LIST_HOST = "productList"
private const val PRODUCT_DETAILS_HOST = "productDetails"

/**
 * Turns a supported deep link URI into one of the type-safe destinations defined in
 * `ui/Routes.kt`, or `null` when the URI is not recognised.
 *
 * Supported patterns:
 *   `$DEEP_LINK_SCHEME://productList?query=<value>`      → [ProductListRoute]
 *   `$DEEP_LINK_SCHEME://productDetails?productId=<value>` → [ProductDetailsRoute]
 *
 * A missing query parameter returns `null` — the App composable then leaves the current
 * destination alone rather than opening a screen with garbage arguments.
 */
fun parseDeepLink(uri: String): Any? {
    val separator = "://"
    val schemeEnd = uri.indexOf(separator)
    if (schemeEnd == -1) return null
    if (uri.substring(0, schemeEnd) != DEEP_LINK_SCHEME) return null

    val remainder = uri.substring(schemeEnd + separator.length)
    val queryStart = remainder.indexOf('?')
    val host = if (queryStart == -1) remainder else remainder.substring(0, queryStart)
    val parameters = if (queryStart == -1) {
        emptyMap()
    } else {
        parseQuery(remainder.substring(queryStart + 1))
    }

    return when (host) {
        PRODUCT_LIST_HOST -> parameters["query"]?.let(::ProductListRoute)
        PRODUCT_DETAILS_HOST -> parameters["productId"]
            ?.takeIf { it.isNotBlank() }
            ?.let(::ProductDetailsRoute)
        else -> null
    }
}

private fun parseQuery(raw: String): Map<String, String> {
    if (raw.isEmpty()) return emptyMap()
    return buildMap {
        // Filter empties upfront so the loop body carries a single short-circuit — a query
        // string like `a=1&&b=2` splits to an empty middle pair that would otherwise need
        // its own `continue`.
        for (pair in raw.split('&').filter { it.isNotEmpty() }) {
            val eq = pair.indexOf('=')
            val rawKey = if (eq == -1) pair else pair.substring(0, eq)
            val rawValue = if (eq == -1) "" else pair.substring(eq + 1)
            val key = rawKey.decodeURLQueryComponent(plusIsSpace = true)
            if (key.isEmpty()) continue
            // putIfAbsent semantics: first occurrence wins, matching typical browser behavior
            // for repeated query keys.
            if (!containsKey(key)) {
                put(key, rawValue.decodeURLQueryComponent(plusIsSpace = true))
            }
        }
    }
}

/**
 * Buffers deep link URIs handed in by platform intent bridges (Android's `Intent.getData()` in
 * `MainActivity`; iOS's SwiftUI `.onOpenURL`) so the App composable can consume them from a
 * `LaunchedEffect`.
 *
 * A [Channel] rather than a `SharedFlow`: each URI represents a one-off navigation, and
 * `receiveAsFlow` delivers each element to exactly one collector. That also means an
 * unconsumed cold-start URI queued before the App mounts is still delivered when collection
 * starts, without being replayed on later recompositions.
 */
class DeepLinkHandler {

    private val channel = Channel<String>(capacity = Channel.BUFFERED)

    /** Consumed by `App.kt`. */
    val uris: Flow<String> = channel.receiveAsFlow()

    /** Called from platform intent bridges. Non-blocking; drops if the buffer overflows. */
    fun push(uri: String) {
        channel.trySend(uri)
    }
}
