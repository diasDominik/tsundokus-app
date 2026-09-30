package uk.tsundokus.features.orders.presentation.reminders

import org.jetbrains.compose.resources.getString
import org.koin.core.annotation.Single
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.reminder_delayed_body
import tsundokuapp.features.orders.presentation.generated.resources.reminder_delayed_title
import tsundokuapp.features.orders.presentation.generated.resources.reminder_overdue_body
import tsundokuapp.features.orders.presentation.generated.resources.reminder_overdue_title
import uk.tsundokus.features.orders.presentation.components.fmtDate
import uk.tsundokus.features.orders.presentation.navigation.orderDeepLink

/** Puts a planned reminder into words, in the device's language. */
fun interface ReminderTexts {
    suspend fun reminderFor(planned: PlannedReminder): Reminder
}

@Single(binds = [ReminderTexts::class])
class ResourceReminderTexts : ReminderTexts {
    override suspend fun reminderFor(planned: PlannedReminder): Reminder =
        Reminder(
            id = planned.id,
            date = planned.date,
            title =
                when (planned.kind) {
                    ReminderKind.OVERDUE -> {
                        getString(Res.string.reminder_overdue_title, planned.orderLabel)
                    }

                    ReminderKind.DELAYED_DATE_REACHED -> {
                        getString(
                            Res.string.reminder_delayed_title,
                            planned.orderLabel,
                        )
                    }
                },
            body =
                when (planned.kind) {
                    ReminderKind.OVERDUE -> {
                        getString(
                            Res.string.reminder_overdue_body,
                            fmtDate(planned.expectedDate),
                        )
                    }

                    ReminderKind.DELAYED_DATE_REACHED -> {
                        getString(Res.string.reminder_delayed_body)
                    }
                },
            deepLink = orderDeepLink(planned.orderId),
        )
}
