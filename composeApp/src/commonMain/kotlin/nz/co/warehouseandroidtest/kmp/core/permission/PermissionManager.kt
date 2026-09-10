package nz.co.warehouseandroidtest.kmp.core.permission

import androidx.compose.runtime.Composable

/**
 * Remembers a [PermissionManager] scoped to the composition and the platform's activity/scene.
 *
 * [onResult] fires whenever the observed [PermissionStatus] changes:
 *   - after `requestPermission()` completes, with the OS's answer, and
 *   - after the app returns to the foreground, so a permission the user flipped in the OS
 *     Settings app is picked up the moment they come back.
 *
 * The initial status is not delivered through the callback — call [PermissionManager.getStatus]
 * once from a `LaunchedEffect` if the initial value matters.
 */
@Composable
expect fun rememberPermissionManager(
    permissionType: PermissionType,
    onResult: (PermissionStatus) -> Unit,
): PermissionManager

/** Imperative side of the API. [rememberPermissionManager] is where you get one. */
interface PermissionManager {

    /**
     * Shows the OS permission dialog if possible. When the current status is
     * [PermissionStatus.PERMANENTLY_DENIED] no dialog will appear; consumers should route the
     * user to [openSystemSettings] in that case.
     */
    fun requestPermission()

    /** Shorthand for `getStatus() == PermissionStatus.GRANTED`. */
    fun isPermissionGranted(): Boolean

    /** Opens the OS-level app settings page so the user can flip a denied permission. */
    fun openSystemSettings()

    /** The most recently observed [PermissionStatus]. Recomputed on lifecycle resume. */
    fun getStatus(): PermissionStatus
}
