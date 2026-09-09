package nz.co.warehouseandroidtest.kmp.ui

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/**
 * Base for screen state holders.
 *
 * Every user action passes through [onIntent], which is the point of the whole arrangement:
 * analytics hooks there once and cannot miss an action, including ones added later. With a
 * method per action, each new method is another chance to forget the tracking call.
 *
 * State is exposed as a single [StateFlow] so a screen cannot observe a half-updated
 * combination. The legacy search screen kept its list in the Activity and its footer state in
 * the adapter, which is exactly how "loading" and "refreshing" got to disagree.
 *
 * [E] is the one-shot event type. Screens that have none use `Nothing`, which satisfies the
 * bound and makes [sendEffect] uncallable.
 */
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(
    initialState: S,
) : ViewModel() {

    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    /**
     * A Channel rather than a StateFlow: each effect is delivered to exactly one collector and
     * then gone, so returning to a screen does not replay the navigation that left it.
     */
    private val _effects = Channel<E>(Channel.BUFFERED)
    val effects: Flow<E> = _effects.receiveAsFlow()

    protected val currentState: S get() = _state.value

    /**
     * The single entry point for user actions. Analytics will be added here; nothing else needs
     * to change when it is.
     */
    fun onIntent(intent: I) {
        handleIntent(intent)
    }

    protected abstract fun handleIntent(intent: I)

    protected fun setState(reducer: S.() -> S) {
        _state.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        _effects.trySend(effect)
    }
}
