package uk.tsundokus.core.presentation.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** No notifications while the app is closed here, so reminders are not offered at all. */
actual fun areRemindersSupported(): Boolean = false

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    remember {
        object : NotificationPermissionRequester {
            override fun request(onResult: (granted: Boolean) -> Unit) = onResult(false)

            override fun openSystemSettings() = Unit
        }
    }
