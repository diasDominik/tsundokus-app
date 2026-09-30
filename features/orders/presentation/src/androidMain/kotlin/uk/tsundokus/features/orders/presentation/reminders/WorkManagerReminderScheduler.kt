package uk.tsundokus.features.orders.presentation.reminders

import android.content.Context
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import java.time.Clock
import java.util.concurrent.TimeUnit

/** The last set handed to the scheduler, kept so it can be recomputed without the app running. */
@Serializable
internal data class ScheduledReminders(
    val reminders: List<StoredReminder>,
    val hour: Int,
    val minute: Int,
)

@Serializable
internal data class StoredReminder(
    val id: String,
    val date: String,
    val title: String,
    val body: String,
    val deepLink: String,
)

/**
 * One WorkManager job per reminder, delayed until its moment. WorkManager keeps them across reboots and
 * app updates and needs no exact-alarm permission — a reminder a few minutes late is fine, and kinder
 * to the battery.
 *
 * A delay is a fixed length of time, so a job scheduled for 9:00 in one time zone would still fire
 * after the same delay in another. The set is therefore kept, and [ReminderRescheduleReceiver]
 * schedules it again whenever the time zone or the clock changes.
 */
class WorkManagerReminderScheduler(
    private val context: Context,
) : ReminderScheduler {
    override suspend fun replaceAll(
        reminders: List<Reminder>,
        hour: Int,
        minute: Int,
    ) {
        remember(ScheduledReminders(reminders.map { it.toStored() }, hour, minute))
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag(TAG)
        // Read now, not kept: after a time zone change the new zone is the one that counts.
        val clock = Clock.systemDefaultZone()
        reminders.forEach { reminder ->
            val delay = reminderDelayMillis(reminder.date, hour, minute, clock) ?: return@forEach
            val request =
                OneTimeWorkRequestBuilder<ReminderWorker>()
                    .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                    .setInputData(
                        workDataOf(
                            ReminderWorker.KEY_ID to reminder.id,
                            ReminderWorker.KEY_TITLE to reminder.title,
                            ReminderWorker.KEY_BODY to reminder.body,
                            ReminderWorker.KEY_DEEP_LINK to reminder.deepLink,
                        ),
                    ).addTag(TAG)
                    .build()
            workManager.enqueueUniqueWork(reminder.id, ExistingWorkPolicy.REPLACE, request)
        }
    }

    /** Schedules the last set again, against the current time zone and clock. */
    suspend fun rescheduleRemembered() {
        val scheduled = remembered() ?: return
        replaceAll(scheduled.reminders.map { it.toReminder() }, scheduled.hour, scheduled.minute)
    }

    private fun remember(scheduled: ScheduledReminders) {
        preferences().edit { putString(KEY_SCHEDULED, Json.encodeToString(scheduled)) }
    }

    private fun remembered(): ScheduledReminders? {
        val stored = preferences().getString(KEY_SCHEDULED, null) ?: return null
        // A set written by an older version that no longer decodes is rebuilt on the next app start.
        return try {
            Json.decodeFromString<ScheduledReminders>(stored)
        } catch (_: SerializationException) {
            null
        }
    }

    private fun preferences() = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    private companion object {
        const val TAG = "reminders"
        const val PREFERENCES = "reminders"
        const val KEY_SCHEDULED = "scheduled"
    }
}

private fun Reminder.toStored() = StoredReminder(id, date, title, body, deepLink)

private fun StoredReminder.toReminder() = Reminder(id, date, title, body, deepLink)
