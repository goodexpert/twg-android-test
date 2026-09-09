package nz.co.warehouseandroidtest.kmp.storage

import com.russhwolf.settings.Settings

/**
 * Ordinary, unencrypted key-value storage, for data that is not a credential.
 *
 * Search history goes here rather than in the secure store: it is not a secret, and the
 * Keychain would be actively wrong for it, since Keychain entries survive an app being deleted.
 */
expect fun createSettings(name: String): Settings
