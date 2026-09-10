package nz.co.warehouseandroidtest.kmp.core.permission

/**
 * Hardware and system permission types the manager supports.
 *
 * A value listed here may still be a work-in-progress on some platforms. An unimplemented
 * combination is expected to fail gracefully — [rememberPermissionManager] returns a manager
 * whose `getStatus()` is [PermissionStatus.NOT_DETERMINED] and whose `requestPermission()`
 * is a no-op — rather than throw.
 */
enum class PermissionType {
    CAMERA,
    LOCATION,
}
