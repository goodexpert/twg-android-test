package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.http.HttpHeaders
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class WarehouseApiTest {

    private val loginBody = """
        {
          "customerId": "cust-123",
          "guest": true,
          "expiryMinutes": 30,
          "isStaff": false,
          "apiVersion": 1.0,
          "somethingTheModelDoesNotDeclare": "ignored"
        }
    """.trimIndent()

    private fun jsonEngine(body: String) = MockEngine { request ->
        lastRequestMethod = request.method
        lastRequestPath = request.url.encodedPath
        lastRequestHeaders = request.headers.entries().associate { it.key to it.value.first() }
        respond(
            content = body,
            status = HttpStatusCode.OK,
            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
        )
    }

    private var lastRequestMethod: HttpMethod? = null
    private var lastRequestPath: String? = null
    private var lastRequestHeaders: Map<String, String> = emptyMap()

    @Test
    fun parsesTheLoginResponse() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(loginBody)))

        val user = api.loginAsGuest().getOrThrow()

        assertEquals("cust-123", user.customerId)
        assertEquals(30, user.expiryMinutes)
        assertTrue(user.guest)
    }

    @Test
    fun toleratesUnknownFields() = runTest {
        // The body above carries a field User does not declare. Strict parsing would fail.
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(loginBody)))

        assertTrue(api.loginAsGuest().isSuccess)
    }

    @Test
    fun getsTheLoginPath() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(loginBody)))

        api.loginAsGuest()

        assertEquals(HttpMethod.Get, lastRequestMethod)
        assertEquals("/twgCSharpTest/Login.json", lastRequestPath)
    }

    @Test
    fun sendsTheGuestAuthorizationHeader() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(loginBody)))

        api.loginAsGuest()

        assertEquals("Guest", lastRequestHeaders["Authorization"])
    }

    @Test
    fun sendsTheSubscriptionKeyAndDeviceHeaders() = runTest {
        val api = KtorWarehouseApi(createWarehouseHttpClient(jsonEngine(loginBody)))

        api.loginAsGuest()

        // The key's value depends on the build environment, so assert only that it is sent.
        assertNotNull(lastRequestHeaders[HEADER_SUBSCRIPTION_KEY])
        assertTrue(lastRequestHeaders.getValue(HEADER_DEVICE) in setOf("Android", "iOS"))
    }

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
