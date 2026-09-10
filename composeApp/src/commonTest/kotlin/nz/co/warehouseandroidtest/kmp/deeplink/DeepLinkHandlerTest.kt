package nz.co.warehouseandroidtest.kmp.deeplink

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class DeepLinkHandlerTest {

    @Test
    fun deliversUrisPushedBeforeCollection() = runTest {
        val handler = DeepLinkHandler()

        // Cold start: platform bridge pushes before anything on the Compose side has read.
        handler.push("$DEEP_LINK_SCHEME://productDetails?productId=R2450827")

        val received = handler.uris.take(1).toList()

        assertEquals(
            listOf("$DEEP_LINK_SCHEME://productDetails?productId=R2450827"),
            received,
        )
    }

    @Test
    fun deliversEachUriExactlyOnce() = runTest {
        val handler = DeepLinkHandler()
        val collected = async { handler.uris.take(2).toList() }

        handler.push("$DEEP_LINK_SCHEME://productList?query=hammer")
        handler.push("$DEEP_LINK_SCHEME://productDetails?productId=R2450827")

        assertEquals(
            listOf(
                "$DEEP_LINK_SCHEME://productList?query=hammer",
                "$DEEP_LINK_SCHEME://productDetails?productId=R2450827",
            ),
            collected.await(),
        )
    }
}
