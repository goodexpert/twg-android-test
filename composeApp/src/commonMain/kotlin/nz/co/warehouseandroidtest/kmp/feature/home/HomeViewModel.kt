package nz.co.warehouseandroidtest.kmp.feature.home

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import nz.co.warehouseandroidtest.kmp.session.SessionRepository
import nz.co.warehouseandroidtest.kmp.ui.BaseViewModel

/**
 * Establishing the session here is TEMPORARY. It is a warm-up, not a guarantee — a session can
 * expire while the app is open. The guarantee is calling `ensureSession()` immediately before
 * each request that needs a UserID, which is where it moves once SearchRepository and
 * ProductRepository exist. The failure handling stays either way.
 *
 * Holding it in a view model rather than the composable means the check survives recomposition
 * and runs on `viewModelScope`, so it is cancelled with the screen instead of the composition.
 */
class HomeViewModel(
    private val sessionRepository: SessionRepository,
) : BaseViewModel<HomeUiState, HomeIntent, Nothing>(HomeUiState()) {

    init {
        checkSession()
    }

    override fun handleIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.RetryLogin -> checkSession()
        }
    }

    private fun checkSession() {
        viewModelScope.launch {
            setState { copy(isCheckingSession = true, sessionFailed = false) }
            val result = sessionRepository.ensureSession()
            setState { copy(isCheckingSession = false, sessionFailed = result.isFailure) }
        }
    }
}
