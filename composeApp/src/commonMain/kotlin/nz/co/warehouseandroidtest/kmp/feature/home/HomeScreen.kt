package nz.co.warehouseandroidtest.kmp.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import nz.co.warehouseandroidtest.kmp.AppContainer
import nz.co.warehouseandroidtest.kmp.currentPlatform

/** Matches the wording the legacy `MainActivity` puts in its Toast, to keep parity. */
private const val LOGIN_FAILED_MESSAGE = "Get User failed!"

/**
 * The two destinations mirror the legacy `MainActivity`. The scanner screen opens, but decodes
 * nothing until the barcode reader is ported.
 */
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenScanner: () -> Unit,
    onOpenSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container.sessionRepository) }
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // The legacy app showed a Toast and carried on with a null UserID. Indefinite with a Retry
    // so the failure is not a dead end: without a session, nothing that needs a UserID works.
    LaunchedEffect(state.sessionFailed) {
        if (!state.sessionFailed) return@LaunchedEffect

        val action = snackbarHostState.showSnackbar(
            message = LOGIN_FAILED_MESSAGE,
            actionLabel = "Retry",
            duration = SnackbarDuration.Indefinite,
        )
        if (action == SnackbarResult.ActionPerformed) {
            viewModel.onIntent(HomeIntent.RetryLogin)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Warehouse",
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = "Running on ${currentPlatform().name}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(
                onClick = onOpenScanner,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Scan barcode")
            }
            Button(
                onClick = onOpenSearch,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Search")
            }
            Text(
                text = "The scanner has no camera yet; it simulates a scan instead.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            if (state.isCheckingSession) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        }
    }
}
