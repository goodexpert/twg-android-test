package nz.co.warehouseandroidtest.kmp.core.permission

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVAuthorizationStatusDenied
import platform.AVFoundation.AVAuthorizationStatusNotDetermined
import platform.AVFoundation.AVAuthorizationStatusRestricted
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.authorizationStatusForMediaType
import platform.AVFoundation.requestAccessForMediaType
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

@Composable
actual fun rememberPermissionManager(
    permissionType: PermissionType,
    onResult: (PermissionStatus) -> Unit,
): PermissionManager {
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnResult by rememberUpdatedState(onResult)

    // Keyed on permissionType so a change of type does not keep the previous type's state.
    var status by remember(permissionType) {
        mutableStateOf(getPermissionStatus(permissionType))
    }

    // The user may leave for Settings and change the permission there. Compose Multiplatform
    // for iOS routes UIScene/application lifecycle through the same LocalLifecycleOwner, so
    // ON_RESUME fires when the app comes back to the foreground.
    DisposableEffect(lifecycleOwner, permissionType) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val refreshed = getPermissionStatus(permissionType)
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
                requestIosPermission(permissionType) { newStatus ->
                    if (newStatus != status) {
                        status = newStatus
                        currentOnResult(newStatus)
                    }
                }
            }

            override fun isPermissionGranted(): Boolean = status == PermissionStatus.GRANTED

            override fun getStatus(): PermissionStatus = status

            override fun openSystemSettings() {
                openAppSettings()
            }
        }
    }
}

private fun getPermissionStatus(
    permissionType: PermissionType,
): PermissionStatus = when (permissionType) {
    PermissionType.CAMERA -> {
        when (AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo)) {
            AVAuthorizationStatusAuthorized -> PermissionStatus.GRANTED
            AVAuthorizationStatusNotDetermined -> PermissionStatus.NOT_DETERMINED
            AVAuthorizationStatusDenied,
            AVAuthorizationStatusRestricted -> PermissionStatus.PERMANENTLY_DENIED
            else -> PermissionStatus.DENIED
        }
    }

    // LOCATION lives here as a placeholder for the shared enum; CoreLocation wiring is a
    // separate piece of work. Returning NOT_DETERMINED with a no-op request keeps callers
    // from crashing until it lands.
    PermissionType.LOCATION -> PermissionStatus.NOT_DETERMINED
}

private fun requestIosPermission(
    permissionType: PermissionType,
    onResult: (PermissionStatus) -> Unit,
) {
    when (permissionType) {
        PermissionType.CAMERA -> {
            AVCaptureDevice.requestAccessForMediaType(mediaType = AVMediaTypeVideo) { granted ->
                // AVFoundation's completion block runs on a background queue; hop back to main
                // before touching Compose state.
                dispatch_async(dispatch_get_main_queue()) {
                    // iOS never re-prompts once the user has answered, so a "no" here is
                    // effectively permanent — Settings is the only way back.
                    onResult(
                        if (granted) PermissionStatus.GRANTED
                        else PermissionStatus.PERMANENTLY_DENIED,
                    )
                }
            }
        }

        PermissionType.LOCATION -> Unit
    }
}

private fun openAppSettings() {
    NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
        UIApplication.sharedApplication.openURL(
            url,
            options = emptyMap<Any?, Any?>(),
            completionHandler = null,
        )
    }
}
