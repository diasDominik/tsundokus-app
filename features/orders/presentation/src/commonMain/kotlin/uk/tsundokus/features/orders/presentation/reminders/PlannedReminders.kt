package uk.tsundokus.features.orders.presentation.reminders

import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import uk.tsundokus.features.orders.domain.dates.parseIsoDate
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.stats.ShippingTimes
import uk.tsundokus.features.orders.domain.stats.shippingTimes

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
    /** [expectedDate] was estimated from past deliveries, the order having no ETA of its own. */
    val isEstimate: Boolean = false,
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
    val shippingTimes = orders.shippingTimes()
    return orders
        .filter { it.status in STILL_COMING }
        .flatMap { order -> remindersFor(order, settings, shippingTimes) }
        .filter { it.date >= earliest }
        .sortedWith(compareBy(PlannedReminder::date, PlannedReminder::id))
        .take(MAX_REMINDERS)
}

private fun remindersFor(
    order: Order,
    settings: ReminderSettings,
    shippingTimes: ShippingTimes,
): List<PlannedReminder> {
    val label = listOf(order.title, order.volume).filter(String::isNotBlank).joinToString(" ")
    // A delayed order is expected on its new date; any other on its ETA, or — shipped without one —
    // when its past deliveries say.
    val estimate = shippingTimes.estimateFor(order)
    val expected =
        if (order.status == OrderStatus.DELAYED &&
            order.delayedTo.isNotBlank()
        ) {
            order.delayedTo
        } else {
            order.eta.ifBlank { estimate?.date.orEmpty() }
        }
    return listOfNotNull(
        isoPlusDays(expected, OVERDUE_AFTER_DAYS)
            ?.takeIf { settings.overdue }
            ?.let { date ->
                PlannedReminder(
                    id = "overdue-${order.id}",
                    orderId = order.id,
                    kind = ReminderKind.OVERDUE,
                    date = date,
                    orderLabel = label,
                    expectedDate = expected,
                    isEstimate = estimate != null,
                )
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
