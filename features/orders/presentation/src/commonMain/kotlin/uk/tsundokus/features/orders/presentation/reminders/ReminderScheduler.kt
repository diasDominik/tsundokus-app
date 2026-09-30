package uk.tsundokus.features.orders.presentation.reminders

/** A reminder ready to hand to the system: shown on [date] (ISO) at the user's chosen time. */
data class Reminder(
    val id: String,
    val date: String,
    val title: String,
    val body: String,
    /** Opened when the notification is tapped. */
    val deepLink: String,
)

/** Schedules reminders with the platform. Android and iOS deliver them; elsewhere this does nothing. */
interface ReminderScheduler {
    /**
     * Makes [reminders] the complete set: anything scheduled before and not in it is cancelled. Each
     * fires at [hour]:[minute] local time on its date; one whose moment has passed is skipped.
     */
    suspend fun replaceAll(
        reminders: List<Reminder>,
        hour: Int,
        minute: Int,
    )
}
