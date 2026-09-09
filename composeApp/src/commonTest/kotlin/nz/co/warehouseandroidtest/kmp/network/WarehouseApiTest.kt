package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import nz.co.warehouseandroidtest.kmp.TestFixtures
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The response bodies are real ones, trimmed, kept as JSON under `src/commonTest/fixtures` and
 * generated into [TestFixtures] by the build. Pasting a fresh response from the API means
 * replacing a file, not re-escaping a Kotlin string.
 */
class WarehouseApiTest {

    private fun jsonEngine(body: String) = MockEngine { request ->
        lastRequestMethod = request.method
        lastRequestPath = request.url.encodedPath
        lastRequestQuery = request.url.parameters.entries()
            .associate { it.key to it.value.first() }
        lastRequestHeaders = request.headers.entries().associate { it.key to it.value.first() }
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private var lastRequestMethod: HttpMethod? = null
    private var lastRequestPath: String? = null
    private var lastRequestQuery: Map<String, String> = emptyMap()
    private var lastRequestHeaders: Map<String, String> = emptyMap()

    @Test
    fun parsesTheLoginResponse() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.loginJson)))

        val user = api.loginAsGuest().getOrThrow()

        assertEquals("cust-123", user.customerId)
        assertEquals(30, user.expiryMinutes)
        assertTrue(user.guest)
        // Guest sessions come back with these; the session store reads expiryMinutes, but the
        // rest have to parse for it to get that far.
        assertEquals(emptyList(), user.preferredBranchIds)
        assertEquals("2026-09-09T06:29:07Z", user.expiresDatetime)
        assertEquals("QAT", user.platformDemandWare)
        assertEquals(1.0, user.apiVersion)
    }

    @Test
    fun toleratesUnknownFields() = runTest {
        // login.json carries `somethingTheModelDoesNotDeclare`, which User does not declare.
        // It is there on purpose — it stands for the keys the API adds later — so leave it in
        // the fixture: without it, strict parsing would never be exercised.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.loginJson)))

        assertTrue(api.loginAsGuest().isSuccess)
    }

    @Test
    fun getsTheLoginPath() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.loginJson)))

        api.loginAsGuest()

        assertEquals(HttpMethod.Get, lastRequestMethod)
        assertEquals("/twgCSharpTest/Login.json", lastRequestPath)
    }

    @Test
    fun sendsTheGuestAuthorizationHeader() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.loginJson)))

        api.loginAsGuest()

        assertEquals("Guest", lastRequestHeaders["Authorization"])
    }

    @Test
    fun sendsTheSubscriptionKeyAndDeviceHeaders() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.loginJson)))

        api.loginAsGuest()

        // The key's value depends on the build environment, so assert only that it is sent.
        assertNotNull(lastRequestHeaders[HEADER_SUBSCRIPTION_KEY])
        assertTrue(lastRequestHeaders.getValue(HEADER_DEVICE) in setOf("Android", "iOS"))
    }

    @Test
    fun parsesTheProductResponse() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val product = assertNotNull(api.getProduct("R2436546").getOrThrow().product)

        assertEquals("R2436546", product.productId)
        assertEquals("Reflex Premium Copy Paper 80gsm 500 Sheet Ream A4", product.productName)
        assertEquals(9.99, product.priceInfo?.price)
        assertEquals("9311995048689", product.productBarcode)
        assertEquals("REFLEX", product.brandCode)
        assertEquals("Reflex", product.brandDescription)
        assertEquals("https://www.thewarehouse.co.nz/s/twl/product/R2436546.html", product.productUrl)
        assertEquals(9, product.featureList.size)
        assertEquals("500 sheets", product.featureList.first())
    }

    @Test
    fun resolvesTheProductsComputedProperties() = runTest {
        // The wire leaves these awkward: a Double price, availability nested in inventory.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val product = assertNotNull(api.getProduct("R2436546").getOrThrow().product)

        assertEquals("${'$'}9.99", product.formattedPrice)
        assertTrue(product.isAvailable)
        assertEquals("Reflex", product.brand)
        assertEquals("9311995048689", product.barcode)
        assertEquals(1, product.images.size)
    }

    @Test
    fun parsesTheProductsInventory() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val inventory = assertNotNull(
            api.getProduct("R2436546").getOrThrow().product?.inventory
        )

        assertTrue(inventory.available)
        assertEquals(408, inventory.soh)
        // In stock, so neither of the "orderable but not today" states applies.
        assertFalse(inventory.preorderable)
        assertFalse(inventory.backorderable)
    }

    @Test
    fun parsesTheProductsImages() = runTest {
        // Unlike a search result, the detail response carries both the flat list and the
        // grouped one, and they point at different hosts for the same picture.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val product = assertNotNull(api.getProduct("R2436546").getOrThrow().product)

        assertTrue(product.imageUrls.single().endsWith("R2436546_40.jpg"))
        assertTrue(product.imageGroups.single().imageUrls.single().endsWith("R2436546_40.jpg"))
        assertEquals("", product.imageGroups.single().colourAttribute)
    }

    @Test
    fun parsesTheProductsPromotions() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val promotions = assertNotNull(api.getProduct("R2436546").getOrThrow().product)
            .promotions

        assertEquals(6, promotions.size)
        val first = promotions.first()
        assertEquals("marketclub-sandbox-5off", first.promotionId)
        assertEquals("5% off your order. Download our app and join MarketClub", first.description)
        assertEquals(9.99, first.price)
        assertEquals("This is the extra detail", first.demandwareConditionsText)
        assertEquals(listOf("ExcludeFromRefinement"), first.tags)
        assertFalse(first.isMarketClubExclusive)
        // Most offers carry no conditions line, so the field has to tolerate being absent.
        assertNull(promotions[1].demandwareConditionsText)
    }

    @Test
    fun parsesTheProductsTextFlags() = runTest {
        // "Y" and "O" arrive as strings. They stay strings: a boolean here would be a guess
        // about the values nobody has seen yet.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        val product = assertNotNull(api.getProduct("R2436546").getOrThrow().product)

        assertEquals("Y", product.soldOnline)
        assertEquals("O", product.clickAndCollect)
        assertTrue(product.isClickAndCollect)
        assertEquals("A4", product.sizeAttribute)
        assertEquals("Standard", product.shippingSize)
        assertEquals("REF4080I", product.manufacturerSku)
        assertEquals("489827", product.mdmProductId)
        // Sent only for a product with colour variants, and this one has none.
        assertNull(product.colourAttribute)
        assertNull(product.refinementColour)
    }

    @Test
    fun sendsTheProductIdAsAQueryParameter() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        api.getProduct("R2436546")

        assertEquals(HttpMethod.Get, lastRequestMethod)
        assertEquals("/twgCSharpTest/Product.json", lastRequestPath)
        // Capitalised, as the endpoint expects; not the camelCase the response uses.
        assertEquals("R2436546", lastRequestQuery["ProductId"])
    }

    @Test
    fun productCallCarriesNoGuestAuthorization() = runTest {
        // Product authenticates on the subscription key alone, unlike Login.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.productJson)))

        api.getProduct("R2436546")

        assertNull(lastRequestHeaders["Authorization"])
        assertNotNull(lastRequestHeaders[HEADER_SUBSCRIPTION_KEY])
    }

    @Test
    fun productFailsOnServerError() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = KtorWarehouseApi(createWarehouseHttpClient(engine))

        assertTrue(api.getProduct("R2436546").isFailure)
    }

    @Test
    fun parsesTheProductsInTheSearchResult() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.searchJson)))

        val product = api.getSearchResult("Paper", 0, 5, "cust-123").getOrThrow()
            .products.first()

        assertEquals("R2436546", product.productId)
        assertEquals("Reflex Premium Copy Paper 80gsm 500 Sheet Ream A4", product.productName)
        // The same type the detail endpoint returns, so the computed properties come along.
        assertEquals("${'$'}9.99", product.formattedPrice)
        assertTrue(product.isAvailable)
        // Search results carry imageGroups but no flat imageUrls, so this is where a list
        // thumbnail has to come from.
        assertTrue(product.imageUrls.isEmpty())
        assertTrue(
            product.imageGroups.single().imageUrls.single().endsWith("R2436546_40.jpg"),
        )
    }

    @Test
    fun parsesTheSearchResultSetSize() = runTest {
        // total is the whole result set, not the page: paging reads it to know when to stop.
        val result = search()

        assertEquals(4276, result.total)
        // One page of five out of those 4276.
        assertEquals(5, result.products.size)
        assertEquals(
            listOf("R2436546", "R2216076", "R2226928", "R2748449", "R2215515"),
            result.products.map { it.productId },
        )
        assertEquals("Paper", result.searchTerm)
    }

    @Test
    fun parsesTheSearchSuggestions() = runTest {
        val suggestions = search().suggestions

        assertEquals(listOf("Paper Mate", "Direct Paper"), suggestions?.brands)
        assertEquals(7, suggestions?.categories?.size)
        val category = suggestions?.categories?.first()
        assertEquals("Photo Paper", category?.name)
        assertEquals(11, category?.productCount)
        assertEquals("Paper", category?.parentCategoryName)
        // Null in every response seen so far, and the model has to survive that.
        assertNull(category?.sizeChartId)
    }

    @Test
    fun parsesTheFacetsAndSortOptions() = runTest {
        val result = search()

        val soldBy = result.facets.first()
        assertEquals("c_marketplaceItem", soldBy.id)
        assertEquals("Sold By", soldBy.name)
        assertEquals(2, soldBy.totalCount)
        assertEquals("The Warehouse", soldBy.values.first().id)
        assertEquals(4037, soldBy.values.first().count)

        // A price band's id is a range expression, and its name is what a filter row shows.
        val price = assertNotNull(result.facets.firstOrNull { it.id == "price" })
        assertEquals("(0..20)", price.values.first().id)
        assertEquals(2071, price.values.first().count)

        // The fixture keeps two values per facet while totalCount still reports the whole
        // list — the same distinction the model documents, and a reminder that the file is
        // trimmed rather than truncated.
        val categories = assertNotNull(result.facets.firstOrNull { it.id == "cgid" })
        assertEquals(38, categories.totalCount)
        assertEquals(2, categories.values.size)

        assertEquals(8, result.sortOptions.size)
        assertEquals("default-navigation-option", result.sortOptions.first().id)
        assertEquals("Best Match", result.sortOptions.first().name)
    }

    @Test
    fun sendsEveryQueryParameterTheSearchExpects() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.searchJson)))

        api.getSearchResult(query = "Paper", start = 20, limit = 10, userId = "cust-123")

        assertEquals(HttpMethod.Get, lastRequestMethod)
        assertEquals("/twgCSharpTest/Search.json", lastRequestPath)
        assertEquals("Paper", lastRequestQuery["Search"])
        assertEquals("20", lastRequestQuery["Start"])
        assertEquals("10", lastRequestQuery["Limit"])
        assertEquals("cust-123", lastRequestQuery["UserID"])
        // Fixed values, as in the legacy Constants.
        assertEquals("1234567890", lastRequestQuery["MachineID"])
        assertEquals("208", lastRequestQuery["Branch"])
    }

    @Test
    fun searchFailsOnServerError() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = KtorWarehouseApi(createWarehouseHttpClient(engine))

        assertTrue(api.getSearchResult("Paper", 0, 1, "cust-123").isFailure)
    }

    private suspend fun search() =
        KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(TestFixtures.searchJson)))
            .getSearchResult("Paper", 0, 5, "cust-123")
            .getOrThrow()

    @Test
    fun failsOnServerError() = runTest {
        val engine = MockEngine { respondError(HttpStatusCode.InternalServerError) }
        val api = KtorWarehouseApi(createWarehouseHttpClient(engine))

        assertTrue(api.loginAsGuest().isFailure)
    }

    @Test
    fun failsOnMalformedBody() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine("not json at all")))

        assertTrue(api.loginAsGuest().isFailure)
    }
}
