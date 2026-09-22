package uk.tsundokus.core.presentation.util

/**
 * Whether this device can run a passkey ceremony at all.
 *
 * Android's Credential Manager passkey APIs — and the libraries wrapping them — start at API 28,
 * below which the app still runs and still signs in with a password. Nothing passkey-related may be
 * touched when this is false: the check exists to keep those classes from ever being loaded there.
 */
expect fun isPasskeySupported(): Boolean
