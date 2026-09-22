package uk.tsundokus.core.presentation.util

import android.os.Build

/** Credential Manager's passkey APIs, and the libraries over them, require Android 9. */
actual fun isPasskeySupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P
