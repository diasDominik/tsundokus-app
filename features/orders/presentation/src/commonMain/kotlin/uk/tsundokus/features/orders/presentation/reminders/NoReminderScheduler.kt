package uk.tsundokus.features.orders.presentation.reminders

/** For platforms that can't notify while the app is closed: schedules nothing. */
internal object NoReminderScheduler : ReminderScheduler {
    override suspend fun replaceAll(
        reminders: List<Reminder>,
        hour: Int,
        minute: Int,
    ) = Unit
}
