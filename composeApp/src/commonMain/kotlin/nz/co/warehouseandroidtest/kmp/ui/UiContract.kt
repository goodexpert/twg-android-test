package nz.co.warehouseandroidtest.kmp.ui

/** Everything a screen renders, in one immutable object. */
interface UiState

/** Everything a user can do on a screen. Sealed by the implementor, so the set is enumerable. */
interface UiIntent

/**
 * Something that happens once — navigate, show a snackbar — as opposed to something that is
 * true for a while.
 *
 * Kept separate from [UiState] because putting a one-shot in state re-fires it: navigating on
 * `state.submittedQuery != null` would navigate again the moment the user came back, since the
 * field is still set.
 *
 * A screen with no one-shot events uses `Nothing` for its effect type and never sends one.
 */
interface UiEffect
