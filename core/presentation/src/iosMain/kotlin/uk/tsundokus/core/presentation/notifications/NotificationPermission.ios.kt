package uk.tsundokus.core.presentation.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

actual fun areRemindersSupported(): Boolean = true

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester =
    remember {
        object : NotificationPermissionRequester {
            override fun request(onResult: (granted: Boolean) -> Unit) {
                // iOS prompts once; afterwards this answers straight away with the stored choice.
                UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
                ) { granted, _ ->
                    dispatch_async(dispatch_get_main_queue()) { onResult(granted) }
                }
            }

            override fun openSystemSettings() {
                NSURL.URLWithString(UIApplicationOpenSettingsURLString)?.let { url ->
                    UIApplication.sharedApplication.openURL(
                        url,
                        options = emptyMap<Any?, Any>(),
                        completionHandler = null,
                    )
                }
            }
        }
    }
