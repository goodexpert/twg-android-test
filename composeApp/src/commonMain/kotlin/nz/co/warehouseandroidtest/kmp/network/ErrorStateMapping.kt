package nz.co.warehouseandroidtest.kmp.network

import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.http.HttpStatusCode
import nz.co.warehouseandroidtest.kmp.data.ErrorState

/**
 * The one place exceptions become [ErrorState].
 *
 * `expectSuccess = true` on the client is what makes this possible: a non-2xx arrives as a
 * typed [ResponseException] rather than as a body that fails to parse, so the status is still
 * available here.
 *
 * The order matters. The network check runs first because a transport failure has no status to
 * inspect, and the catch-all runs last so an exception nobody anticipated becomes
 * [ErrorState.UnknownErrorState] rather than being reported as a connection problem the user
 * might retry forever.
 */
fun Throwable.toErrorState(): ErrorState = when {
    isNetworkFailure() -> ErrorState.NetworkErrorState

    // A 200 that carried nothing, which is how these endpoints report an unknown id.
    this is NotFoundException -> ErrorState.NotFoundErrorState

    this is ClientRequestException ->
        // 404 is "no such product"; every other 4xx is a request this app built wrong, which
        // is not something the user can act on.
        if (response.status == HttpStatusCode.NotFound) {
            ErrorState.NotFoundErrorState
        } else {
            ErrorState.UnknownErrorState
        }

    this is ServerResponseException -> ErrorState.ServerErrorState

    // A 3xx that survived redirect handling. Nothing sensible to tell the user about it.
    this is ResponseException -> ErrorState.UnknownErrorState

    // Includes JsonConvertException: a body this app cannot read is a model that has fallen
    // behind the API, not a connection problem.
    else -> ErrorState.UnknownErrorState
}
