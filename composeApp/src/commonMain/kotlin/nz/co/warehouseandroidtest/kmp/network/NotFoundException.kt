package nz.co.warehouseandroidtest.kmp.network

/**
 * The request succeeded and there is nothing to show.
 *
 * These endpoints do not always answer a missing record with a 404: `Product.json` returns 200
 * with an empty envelope for an id it does not know. A repository that spots that throws this
 * so the case reaches [toErrorState] as [nz.co.warehouseandroidtest.kmp.data.ErrorState
 * .NotFoundErrorState], the same as a real 404 would.
 */
class NotFoundException(message: String) : Exception(message)
