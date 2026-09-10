package nz.co.warehouseandroidtest.kmp.core.permission

/**
 * The state of a permission from the OS's perspective, normalised across platforms.
 *
 * The mapping is not symmetrical:
 *   - [NOT_DETERMINED] is native on iOS (`AVAuthorizationStatusNotDetermined`). Android has
 *     no equivalent state, so it is inferred from local bookkeeping that remembers whether
 *     the app has ever prompted for the permission before.
 *   - [SHOW_RATIONALE] is Android-only. The user denied once and the system will let us ask
 *     again — a good moment to explain why the permission is needed.
 *   - [PERMANENTLY_DENIED] on Android means the user chose "Don't ask again". On iOS it
 *     means the user denied at any point, since iOS never re-prompts.
 */
enum class PermissionStatus {
    /** The app has never asked; a request will show the OS dialog. */
    NOT_DETERMINED,

    /** The permission is currently granted. */
    GRANTED,

    /** The user denied without disabling future prompts. Reserved for platform behaviour that
     *  reports plain "denied" without a rationale hint. */
    DENIED,

    /** Android only. The user denied once; the OS will allow another prompt. */
    SHOW_RATIONALE,

    /** Only the OS Settings app can flip this. `requestPermission()` will not surface a dialog. */
    PERMANENTLY_DENIED,
}
