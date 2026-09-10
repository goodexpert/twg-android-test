package nz.co.warehouseandroidtest.kmp.core.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun rememberPermissionManager(
    permissionType: PermissionType,
    onResult: (PermissionStatus) -> Unit,
): PermissionManager {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // rememberUpdatedState so a caller that hands us a new lambda on recomposition still gets
    // the latest one inside the launcher/lifecycle callbacks, without us re-keying `remember`.
    val currentOnResult by rememberUpdatedState(onResult)

    // Keyed on permissionType: switching the observed permission cannot carry the previous
    // type's state forward.
    var status by remember(permissionType) {
        mutableStateOf(getPermissionStatus(context, permissionType))
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // The user has now seen the dialog (or the system suppressed it because the permission
        // is permanently denied). Either way, subsequent status checks should stop reporting
        // NOT_DETERMINED — see [getPermissionStatus] for why this flag exists.
        markPromptShown(context, permissionType)
        val next = if (granted) {
            PermissionStatus.GRANTED
        } else {
            getPermissionStatus(context, permissionType)
        }
        if (next != status) {
            status = next
            currentOnResult(next)
        }
    }

    // Re-check on every ON_RESUME so a permission the user flipped in the OS Settings app
    // is picked up when they return. Without this, "Open settings" is a one-way trip.
    DisposableEffect(lifecycleOwner, permissionType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val refreshed = getPermissionStatus(context, permissionType)
                if (refreshed != status) {
                    status = refreshed
                    currentOnResult(refreshed)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return remember(permissionType) {
        object : PermissionManager {

            override fun requestPermission() {
                launcher.launch(permissionType.toAndroidPermission())
            }

            override fun isPermissionGranted(): Boolean = status == PermissionStatus.GRANTED

            override fun getStatus(): PermissionStatus = status

            override fun openSystemSettings() {
                val intent = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", context.packageName, null),
                )
                context.startActivity(intent)
            }
        }
    }
}

private fun getPermissionStatus(
    context: Context,
    permissionType: PermissionType,
): PermissionStatus {
    val permission = permissionType.toAndroidPermission()
    if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
        return PermissionStatus.GRANTED
    }

    // Android has no native "not determined" state: a first-run install looks identical to
    // one whose permission was permanently denied. Our own bookkeeping fills the gap. The
    // flag is set the first time the OS dialog callback fires, so a user who has never seen
    // a prompt stays on NOT_DETERMINED regardless of what `shouldShowRequestPermissionRationale`
    // reports (which is also `false` on a fresh install).
    if (!hasPromptedBefore(context, permissionType)) {
        return PermissionStatus.NOT_DETERMINED
    }

    val activity = context as? Activity ?: return PermissionStatus.DENIED

    return if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
        PermissionStatus.SHOW_RATIONALE
    } else {
        PermissionStatus.PERMANENTLY_DENIED
    }
}

private fun PermissionType.toAndroidPermission(): String = when (this) {
    PermissionType.CAMERA -> Manifest.permission.CAMERA
    PermissionType.LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
}

// SharedPreferences rather than the shared settings graph: this bookkeeping is a purely
// platform-local concern and does not warrant a seam through commonMain.
private const val PREFS_NAME = "warehouse_permission_prompts"
private const val KEY_PREFIX = "prompted_"

private fun hasPromptedBefore(context: Context, permissionType: PermissionType): Boolean =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getBoolean(KEY_PREFIX + permissionType.name, false)

private fun markPromptShown(context: Context, permissionType: PermissionType) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit {
            putBoolean(KEY_PREFIX + permissionType.name, true)
        }
}
