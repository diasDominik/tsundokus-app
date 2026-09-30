package uk.tsundokus.features.orders.presentation.reminders

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import org.jetbrains.compose.resources.getString
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.reminder_channel_description
import tsundokuapp.features.orders.presentation.generated.resources.reminder_channel_name
import uk.tsundokus.features.orders.presentation.R

/** Shows one reminder when its moment comes. Tapping it opens the app on the order it is about. */
class ReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        // Permission can be taken back in the system settings after the reminder was scheduled.
        if (!canNotify()) return Result.success()
        val id = inputData.getString(KEY_ID) ?: return Result.success()
        ensureChannel()
        val notification =
            NotificationCompat
                .Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_reminder)
                .setContentTitle(inputData.getString(KEY_TITLE))
                .setContentText(inputData.getString(KEY_BODY))
                .setStyle(NotificationCompat.BigTextStyle().bigText(inputData.getString(KEY_BODY)))
                .setContentIntent(openOrder(id, inputData.getString(KEY_DEEP_LINK)))
                .setAutoCancel(true)
                .build()
        NotificationManagerCompat.from(applicationContext).notify(id.hashCode(), notification)
        return Result.success()
    }

    private fun canNotify(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** Idempotent: creating an existing channel only updates its name and description. */
    private suspend fun ensureChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(Res.string.reminder_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = getString(Res.string.reminder_channel_description) }
        applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** The app's own launch activity, handed the order's link — it routes it like any deep link. */
    private fun openOrder(
        id: String,
        deepLink: String?,
    ): PendingIntent? {
        val launch =
            applicationContext.packageManager.getLaunchIntentForPackage(applicationContext.packageName)
                ?: return null
        deepLink?.let { launch.data = Uri.parse(it) }
        return PendingIntent.getActivity(
            applicationContext,
            id.hashCode(),
            launch,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        const val KEY_ID = "id"
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_DEEP_LINK = "deepLink"
        private const val CHANNEL_ID = "reminders"
    }
}
