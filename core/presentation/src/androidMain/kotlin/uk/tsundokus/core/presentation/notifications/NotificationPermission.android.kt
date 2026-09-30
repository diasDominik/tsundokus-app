package uk.tsundokus.core.presentation.notifications

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.NotificationManagerCompat

actual fun areRemindersSupported(): Boolean = true

@Composable
actual fun rememberNotificationPermissionRequester(): NotificationPermissionRequester {
    val context = LocalContext.current
    // Remembered, so the callback set by request() survives recompositions until the answer comes.
    val pendingResult = remember { mutableStateOf<((Boolean) -> Unit)?>(null) }
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            pendingResult.value?.invoke(granted)
            pendingResult.value = null
        }
    val currentLauncher = rememberUpdatedState(launcher)
    return remember(context) {
        object : NotificationPermissionRequester {
            override fun request(onResult: (granted: Boolean) -> Unit) {
                // Before Android 13 there is no prompt: notifications are allowed unless the user
                // switched them off for the app, which only the system settings can undo.
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                    onResult(NotificationManagerCompat.from(context).areNotificationsEnabled())
                    return
                }
                pendingResult.value = onResult
                currentLauncher.value.launch(Manifest.permission.POST_NOTIFICATIONS)
            }

            override fun openSystemSettings() {
                context.startActivity(
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }
    }
}
