package nz.co.warehouseandroidtest.kmp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import nz.co.warehouseandroidtest.kmp.data.ErrorState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import twg_android_test.composeapp.generated.resources.Res
import twg_android_test.composeapp.generated.resources.action_try_again
import twg_android_test.composeapp.generated.resources.error_connection_message
import twg_android_test.composeapp.generated.resources.error_connection_title
import twg_android_test.composeapp.generated.resources.error_not_found_message
import twg_android_test.composeapp.generated.resources.error_not_found_title
import twg_android_test.composeapp.generated.resources.error_server_message
import twg_android_test.composeapp.generated.resources.error_server_title
import twg_android_test.composeapp.generated.resources.error_unknown_message
import twg_android_test.composeapp.generated.resources.error_unknown_title

/**
 * What a screen shows in place of its content when a load fails: one message per kind of
 * failure, over [FullPageMessageUi].
 *
 * Shared so that every screen answers a failed load the same way. [screenName] is the only part
 * that differs — the screen's own name, dropped into the copy: "Unable to load Product Details
 * due to a connection error".
 *
 * There is one action, and what it does is the caller's decision. The label stays "Try Again"
 * across all four states because the button is the caller's, not this component's: a screen
 * that wants a different way out of [ErrorState.NotFoundErrorState] passes a different
 * [onAction] rather than this file guessing.
 *
 * The copy lives in `composeResources/values/strings.xml`, which both platforms read.
 */
@Composable
fun ErrorContent(
    error: ErrorState,
    screenName: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pairs, not two parallel `when`s: a state can only ever have one title and one message,
    // and a single expression is what keeps them from drifting apart as states are added.
    val (title, message) = error.messages()

    FullPageMessageUi(
        title = stringResource(title),
        message = stringResource(message, screenName),
        actionLabel = stringResource(Res.string.action_try_again),
        onAction = onAction,
        modifier = modifier,
    )
}

/** The title and message resources for one failure. The message takes the screen name. */
private fun ErrorState.messages(): Pair<StringResource, StringResource> = when (this) {
    ErrorState.NetworkErrorState ->
        Res.string.error_connection_title to Res.string.error_connection_message

    ErrorState.ServerErrorState ->
        Res.string.error_server_title to Res.string.error_server_message

    ErrorState.NotFoundErrorState ->
        Res.string.error_not_found_title to Res.string.error_not_found_message

    ErrorState.UnknownErrorState ->
        Res.string.error_unknown_title to Res.string.error_unknown_message
}
