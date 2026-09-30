package uk.tsundokus.core.presentation.notifications

import androidx.compose.runtime.Composable

/**
 * Whether this platform can show a notification at a set time while the app is closed. Android and
 * iOS can; desktop and the web can only while the app is open, so they don't offer reminders.
 */
expect fun areRemindersSupported(): Boolean

/** Asks the system for permission to show notifications. */
interface NotificationPermissionRequester {
    /** Asks — or answers at once where no prompt is needed — and reports whether it was granted. */
    fun request(onResult: (granted: Boolean) -> Unit)

    /** For after a refusal: only the system settings can grant it then. */
    fun openSystemSettings()
}

@Composable
expect fun rememberNotificationPermissionRequester(): NotificationPermissionRequester
