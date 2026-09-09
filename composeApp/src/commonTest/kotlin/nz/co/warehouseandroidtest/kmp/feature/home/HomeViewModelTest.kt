package nz.co.warehouseandroidtest.kmp.feature.home

import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import nz.co.warehouseandroidtest.kmp.data.User
import nz.co.warehouseandroidtest.kmp.session.FakeWarehouseApi
import nz.co.warehouseandroidtest.kmp.session.SessionRepository
import nz.co.warehouseandroidtest.kmp.session.SessionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * `viewModelScope` runs on `Dispatchers.Main`, which is not the scheduler `runTest` drives.
 * Without [setMain] the session check in `HomeViewModel.init` is dispatched somewhere the test
 * cannot advance, and every assertion races it. Substituting Main makes the launch land on the
 * test scheduler, so `advanceUntilIdle`/`runCurrent` decide when it runs.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private val mainDispatcher = StandardTestDispatcher()

    private val loggedInUser = User(customerId = "cust-123", expiryMinutes = 30)

    @BeforeTest
    fun substituteMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @AfterTest
    fun restoreMainDispatcher() {
        Dispatchers.resetMain()
    }

    private fun repository(api: FakeWarehouseApi) =
        SessionRepository(api, SessionStore(MapSettings()), now = { 1_000_000L })

    @Test
    fun clearsTheCheckingFlagOnceLoginSucceeds() = runTest {
        val api = FakeWarehouseApi(Result.success(loggedInUser))

        val viewModel = HomeViewModel(repository(api))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCheckingSession)
        assertFalse(viewModel.state.value.sessionFailed)
    }

    @Test
    fun reportsFailureWhenLoginFails() = runTest {
        val api = FakeWarehouseApi(Result.failure(RuntimeException("boom")))

        val viewModel = HomeViewModel(repository(api))
        advanceUntilIdle()

        assertFalse(viewModel.state.value.isCheckingSession)
        assertTrue(viewModel.state.value.sessionFailed)
    }

    @Test
    fun checksTheSessionAsSoonAsTheScreenOpens() = runTest {
        // Held, so the login is still in flight when the assertions run.
        val api = FakeWarehouseApi(Result.success(loggedInUser), CompletableDeferred())

        val viewModel = HomeViewModel(repository(api))
        runCurrent()

        assertEquals(1, api.callCount)
        // The spinner has something to show for as long as the login is outstanding.
        assertTrue(viewModel.state.value.isCheckingSession)

        api.release()
        advanceUntilIdle()
        assertFalse(viewModel.state.value.isCheckingSession)
    }

    @Test
    fun retryRunsAnotherLogin() = runTest {
        val api = FakeWarehouseApi(Result.failure(RuntimeException("boom")))
        val viewModel = HomeViewModel(repository(api))
        advanceUntilIdle()
        assertEquals(1, api.callCount)

        viewModel.onIntent(HomeIntent.RetryLogin)
        advanceUntilIdle()

        assertEquals(2, api.callCount)
    }

    @Test
    fun retryClearsTheFailureBeforeTryingAgain() = runTest {
        // Otherwise the screen would keep its old error visible while the retry is in flight.
        val api = FakeWarehouseApi(Result.failure(RuntimeException("boom")))
        val viewModel = HomeViewModel(repository(api))
        advanceUntilIdle()
        assertTrue(viewModel.state.value.sessionFailed)

        api.hold()
        viewModel.onIntent(HomeIntent.RetryLogin)
        runCurrent()

        assertFalse(viewModel.state.value.sessionFailed)
        assertTrue(viewModel.state.value.isCheckingSession)

        // Let the retry finish, so the test does not leave a coroutine parked on the gate.
        api.release()
        advanceUntilIdle()
        assertTrue(viewModel.state.value.sessionFailed)
    }
}
