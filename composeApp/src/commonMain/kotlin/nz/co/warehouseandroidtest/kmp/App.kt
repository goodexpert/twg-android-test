package nz.co.warehouseandroidtest.kmp

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Matches the wording the legacy `MainActivity` puts in its Toast, to keep parity. */
private const val LOGIN_FAILED_MESSAGE = "Get User failed!"

/**
 * Shared entry point, hosted by [nz.co.warehouseandroidtest.kmp.MainActivity] on Android and by
 * `MainViewController()` on iOS.
 *
 * The two destinations mirror the legacy `MainActivity`, but stay disabled until the data layer
 * and navigation are ported.
 */
@Composable
fun App(container: AppContainer) {
    val snackbarHostState = remember { SnackbarHostState() }
    var isCheckingSession by remember { mutableStateOf(true) }
    // Bumped by Retry; LaunchedEffect keys off it, so a new attempt runs.
    var sessionAttempt by remember { mutableIntStateOf(0) }

    // TEMPORARY placement. Establishing the session here is a warm-up, not a guarantee — a
    // session can expire while the app is open. The guarantee is calling ensureSession()
    // immediately before each request that needs a UserID, which is where this moves once
    // SearchRepository and ProductRepository exist. The failure handling below stays.
    LaunchedEffect(sessionAttempt) {
        isCheckingSession = true
        val result = container.sessionRepository.ensureSession()
        isCheckingSession = false

        if (result.isFailure) {
            // The legacy app showed a Toast and carried on with a null UserID. Indefinite with
            // a Retry so the failure is not a dead end: without a session, nothing that needs
            // a UserID can work.
            val action = snackbarHostState.showSnackbar(
                message = LOGIN_FAILED_MESSAGE,
                actionLabel = "Retry",
                duration = SnackbarDuration.Indefinite,
            )
            if (action == SnackbarResult.ActionPerformed) {
                sessionAttempt++
            }
        }
    }

    MaterialTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Scan barcode")
                }
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Search")
                }
                Text(
                    text = "Both destinations are placeholders until the data layer is ported.",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                )
                if (isCheckingSession) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}
