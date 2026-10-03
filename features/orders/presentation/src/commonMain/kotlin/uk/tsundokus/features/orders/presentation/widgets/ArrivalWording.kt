package uk.tsundokus.features.orders.presentation.widgets

import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import uk.tsundokus.features.orders.domain.models.OrderStatus

/** Which sentence an arrival gets on a widget, before it is put into words. */
enum class ArrivalPhrase {
    /** Ordered, waiting for its release. */
    RELEASES,

    /** On the way and not due yet. */
    ARRIVES,

    /** Reported late, to a date still ahead. */
    DELAYED_TO,

    /** Its date has passed and it still hasn't arrived. */
    OVERDUE,
}

/** How a widget names [WidgetArrival.date]: "today", "tomorrow", or the date itself. */
enum class RelativeDay { TODAY, TOMORROW, OTHER }

fun WidgetArrival.phraseOn(today: String): ArrivalPhrase =
    when {
        status == OrderStatus.ORDERED.name -> ArrivalPhrase.RELEASES
        date < today -> ArrivalPhrase.OVERDUE
        status == OrderStatus.DELAYED.name -> ArrivalPhrase.DELAYED_TO
        else -> ArrivalPhrase.ARRIVES
    }

fun relativeDay(
    date: String,
    today: String,
): RelativeDay =
    when (date) {
        today -> RelativeDay.TODAY
        isoPlusDays(today, 1) -> RelativeDay.TOMORROW
        else -> RelativeDay.OTHER
    }
