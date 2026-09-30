package uk.tsundokus.features.orders.presentation.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Reschedules the reminders when the time zone or the clock changes — travel, daylight saving set by
 * hand, a corrected clock — so 9:00 means 9:00 where the phone is now. Runs without the app open.
 */
class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_TIMEZONE_CHANGED && intent.action != Intent.ACTION_TIME_CHANGED) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                WorkManagerReminderScheduler(context.applicationContext).rescheduleRemembered()
            } finally {
                pending.finish()
            }
        }
    }
}
