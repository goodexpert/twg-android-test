package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import nz.co.warehouseandroidtest.kmp.data.ErrorState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Drives real requests through `MockEngine` rather than constructing Ktor's exceptions by
 * hand: the point of the mapping is what the client actually throws, and a hand-built
 * exception would only prove the `when` branches match themselves.
 */
class ErrorStateMappingTest {

    private suspend fun errorStateFor(engine: MockEngine): ErrorState {
        val api = KtorWarehouseApi(createWarehouseHttpClient(engine))
        val failure = api.getProduct("R2436546").exceptionOrNull()
        return assertNotNull(failure, "the call was expected to fail").toErrorState()
    }

    private fun respondingWith(status: HttpStatusCode) = MockEngine { respondError(status) }

    @Test
    fun mapsATransportFailureToNetwork() = runTest {
        // kotlinx.io's IOException is java.io.IOException on the JVM, so this exercises the
        // Android actual as written and the iOS one as the same check.
        val engine = MockEngine { throw IOException("host unreachable") }

        assertEquals(ErrorState.NetworkErrorState, errorStateFor(engine))
    }

    @Test
    fun mapsAServerErrorToServer() = runTest {
        assertEquals(
            ErrorState.ServerErrorState,
            errorStateFor(respondingWith(HttpStatusCode.InternalServerError)),
        )
    }

    @Test
    fun mapsA404ToNotFound() = runTest {
        assertEquals(
            ErrorState.NotFoundErrorState,
            errorStateFor(respondingWith(HttpStatusCode.NotFound)),
        )
    }

    @Test
    fun mapsAnEmptyEnvelopeToNotFound() {
        // The repository's case: 200, but nothing in it. Same answer as a 404.
        assertEquals(
            ErrorState.NotFoundErrorState,
            NotFoundException("no product").toErrorState(),
        )
    }

    @Test
    fun mapsOther4xxToUnknown() = runTest {
        // A request this app built wrong is not something the user can retry their way out of.
        assertEquals(
            ErrorState.UnknownErrorState,
            errorStateFor(respondingWith(HttpStatusCode.Unauthorized)),
        )
    }

    @Test
    fun mapsAnUnreadableBodyToUnknown() = runTest {
        // A parse failure means the models have fallen behind the API. Reporting it as a
        // connection error would send the user retrying forever.
        val engine = MockEngine {
            respond(
                content = "not json at all",
                status = HttpStatusCode.OK,
                headers = headersOf(
                    HttpHeaders.ContentType,
                    ContentType.Application.Json.toString(),
                ),
            )
        }

        assertEquals(ErrorState.UnknownErrorState, errorStateFor(engine))
    }

    @Test
    fun mapsAnythingElseToUnknown() {
        assertEquals(ErrorState.UnknownErrorState, IllegalStateException("boom").toErrorState())
    }
}
