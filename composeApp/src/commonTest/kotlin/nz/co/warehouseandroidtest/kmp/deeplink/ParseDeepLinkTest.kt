package nz.co.warehouseandroidtest.kmp.deeplink

import nz.co.warehouseandroidtest.kmp.ui.ProductDetailsRoute
import nz.co.warehouseandroidtest.kmp.ui.ProductListRoute
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ParseDeepLinkTest {

    @Test
    fun productListUriBecomesProductListRoute() {
        val route = parseDeepLink("$DEEP_LINK_SCHEME://productList?query=hammer")

        assertEquals(ProductListRoute(query = "hammer"), route)
    }

    @Test
    fun productListQueryIsPercentDecoded() {
        val route = parseDeepLink("$DEEP_LINK_SCHEME://productList?query=power%20drill")

        assertEquals(ProductListRoute(query = "power drill"), route)
    }

    @Test
    fun productListPlusIsDecodedAsSpace() {
        // Query-string convention: `+` stands in for a space when `application/x-www-form-urlencoded`.
        val route = parseDeepLink("$DEEP_LINK_SCHEME://productList?query=power+drill")

        assertEquals(ProductListRoute(query = "power drill"), route)
    }

    @Test
    fun productDetailsUriBecomesProductDetailsRoute() {
        val route = parseDeepLink("$DEEP_LINK_SCHEME://productDetails?productId=R2450827")

        assertEquals(ProductDetailsRoute(productId = "R2450827"), route)
    }

    @Test
    fun extraQueryParametersAreIgnored() {
        val route = parseDeepLink(
            "$DEEP_LINK_SCHEME://productDetails?productId=R2450827&utm_source=email"
        )

        assertEquals(ProductDetailsRoute(productId = "R2450827"), route)
    }

    @Test
    fun unknownHostReturnsNull() {
        assertNull(parseDeepLink("$DEEP_LINK_SCHEME://cart?itemId=1"))
    }

    @Test
    fun unknownSchemeReturnsNull() {
        assertNull(parseDeepLink("https://productList?query=hammer"))
    }

    @Test
    fun missingRequiredParameterReturnsNull() {
        assertNull(parseDeepLink("$DEEP_LINK_SCHEME://productList"))
        assertNull(parseDeepLink("$DEEP_LINK_SCHEME://productDetails"))
    }

    @Test
    fun blankProductIdReturnsNull() {
        assertNull(parseDeepLink("$DEEP_LINK_SCHEME://productDetails?productId="))
        assertNull(parseDeepLink("$DEEP_LINK_SCHEME://productDetails?productId=%20"))
    }

    @Test
    fun malformedUriReturnsNull() {
        assertNull(parseDeepLink("not-a-uri"))
        assertNull(parseDeepLink(""))
    }
}
