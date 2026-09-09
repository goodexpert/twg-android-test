package nz.co.warehouseandroidtest.kmp.data

/**
 * Why a call failed, in the terms a screen needs to answer "what do I tell the user".
 *
 * Deliberately coarser than the exceptions behind it: the difference between a DNS failure and
 * a socket timeout does not change what the screen says or offers, so both arrive here as
 * [NetworkErrorState]. The mapping lives in
 * [nz.co.warehouseandroidtest.kmp.network.toErrorState].
 */
sealed interface ErrorState {
    /** No usable connection: unreachable host, timeout, DNS failure. Retrying may work. */
    data object NetworkErrorState : ErrorState

    /** The request arrived and the server failed it — a 5xx. Retrying may work. */
    data object ServerErrorState : ErrorState

    /**
     * The request was understood and there is nothing to show: a 404, or a 200 whose envelope
     * carries no product, which is how `Product.json` answers an id it does not know. Retrying
     * the same request will not change the answer.
     */
    data object NotFoundErrorState : ErrorState

    /**
     * Anything else, including a response this app cannot parse. A parse failure usually means
     * the models have fallen behind the API rather than anything the user did, so it must not
     * be dressed up as a connection problem.
     */
    data object UnknownErrorState : ErrorState
}
