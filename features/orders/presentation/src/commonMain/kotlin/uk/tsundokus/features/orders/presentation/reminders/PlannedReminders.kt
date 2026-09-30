package uk.tsundokus.features.orders.presentation.reminders

import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.components.isoPlusDays
import uk.tsundokus.features.orders.presentation.components.parseIsoDate

enum class ReminderKind {
    /** Three days past the expected date and still not received. */
    OVERDUE,

    /** A delayed order's new date has come round. */
    DELAYED_DATE_REACHED,
}

/** A reminder to show on [date] (ISO `yyyy-MM-dd`), before its words are filled in. */
data class PlannedReminder(
    /** Stable per order and kind, so rescheduling replaces rather than duplicates. */
    val id: String,
    val orderId: String,
    val kind: ReminderKind,
    val date: String,
    /** "One Piece Vol. 107". */
    val orderLabel: String,
    /** The date the order was expected on — what an overdue reminder refers to. */
    val expectedDate: String,
)

/** How long after the expected date an order counts as overdue: a day or two late is normal post. */
internal const val OVERDUE_AFTER_DAYS = 3

/** iOS keeps at most 64 pending notifications per app; the soonest 50 leave room to spare. */
internal const val MAX_REMINDERS = 50

private val STILL_COMING = setOf(OrderStatus.ORDERED, OrderStatus.SHIPPED, OrderStatus.DELAYED)

/**
 * The reminders [orders] call for under [settings], soonest first. Pure, so it runs on every change
 * and the scheduler is only touched when the answer changes.
 *
 * [todayUtc] is today's date in UTC, not the device's: local "today" can be a day either side of it.
 * So reminders from the day before are kept too, and the platform scheduler — which knows the local
 * time — skips any whose moment has already passed.
 */
fun plannedReminders(
    orders: List<Order>,
    settings: ReminderSettings,
    todayUtc: String,
): List<PlannedReminder> {
    if (!settings.enabled) return emptyList()
    val earliest = isoPlusDays(todayUtc, -1) ?: todayUtc
    return orders
        .filter { it.status in STILL_COMING }
        .flatMap { order -> remindersFor(order, settings) }
        .filter { it.date >= earliest }
        .sortedWith(compareBy(PlannedReminder::date, PlannedReminder::id))
        .take(MAX_REMINDERS)
}

private fun remindersFor(
    order: Order,
    settings: ReminderSettings,
): List<PlannedReminder> {
    val label = listOf(order.title, order.volume).filter(String::isNotBlank).joinToString(" ")
    // A delayed order is expected on its new date; any other on its ETA.
    val expected =
        if (order.status == OrderStatus.DELAYED &&
            order.delayedTo.isNotBlank()
        ) {
            order.delayedTo
        } else {
            order.eta
        }
    return listOfNotNull(
        isoPlusDays(expected, OVERDUE_AFTER_DAYS)
            ?.takeIf { settings.overdue }
            ?.let { date ->
                PlannedReminder("overdue-${order.id}", order.id, ReminderKind.OVERDUE, date, label, expected)
            },
        order.delayedTo
            .takeIf {
                settings.delayedDateReached && order.status == OrderStatus.DELAYED &&
                    parseIsoDate(it) != null
            }?.let { date ->
                PlannedReminder(
                    "delayed-${order.id}",
                    order.id,
                    ReminderKind.DELAYED_DATE_REACHED,
                    date,
                    label,
                    date,
                )
            },
    )
}
