package uk.tsundokus.core.presentation.util

/**
 * No platform floor to check here: the passkey client is present on every build of this target, and
 * a device or browser that cannot run a ceremony says so when one is attempted.
 */
actual fun isPasskeySupported(): Boolean = true
