package nz.co.warehouseandroidtest.kmp.session

import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

/**
 * Keychain-backed. Note this outlives an app uninstall, unlike NSUserDefaults — acceptable
 * for a session that carries its own expiry, but worth knowing.
 */
@OptIn(ExperimentalSettingsImplementation::class)
actual fun createSecureSettings(): Settings = KeychainSettings(SERVICE)

private const val SERVICE = "nz.co.warehouseandroidtest.kmp.session"
