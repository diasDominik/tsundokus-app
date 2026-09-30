package uk.tsundokus.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Which reminders this device shows, and when. Kept on the device, not the account: notifications are
 * a per-phone choice. Off until the user turns them on, since that asks for notification permission.
 */
data class ReminderSettings(
    val enabled: Boolean = false,
    /** Three days past an order's expected date, when it still hasn't arrived. */
    val overdue: Boolean = true,
    /** The morning a delayed order's new date comes round. */
    val delayedDateReached: Boolean = true,
    val hour: Int = 9,
    val minute: Int = 0,
)

interface ReminderPreferences {
    fun settings(): Flow<ReminderSettings>

    suspend fun update(settings: ReminderSettings)
}
