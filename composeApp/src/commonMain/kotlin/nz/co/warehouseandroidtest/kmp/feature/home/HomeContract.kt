package nz.co.warehouseandroidtest.kmp.feature.home

import nz.co.warehouseandroidtest.kmp.ui.UiIntent
import nz.co.warehouseandroidtest.kmp.ui.UiState

/**
 * [sessionFailed] is state rather than a one-shot effect: a failed login stays true until it is
 * retried, and the screen keeps its message up for exactly that long.
 */
data class HomeUiState(
    val isCheckingSession: Boolean = true,
    val sessionFailed: Boolean = false,
) : UiState

sealed interface HomeIntent : UiIntent {
    data object RetryLogin : HomeIntent
}
