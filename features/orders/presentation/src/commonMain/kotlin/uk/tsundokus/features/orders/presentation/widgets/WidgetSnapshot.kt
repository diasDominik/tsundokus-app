package uk.tsundokus.features.orders.presentation.widgets

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.tsundokus.features.orders.domain.dates.daysBetween
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.components.arrivalDate

/**
 * What the home-screen widgets show. Widgets run without the app — no DI, no database — so the app
 * works this out and hands it over; the widgets only read it. Dates are ISO `yyyy-MM-dd` so a widget
 * can tell "today" from "tomorrow" whenever it draws, without the app running.
 */
@Serializable
data class WidgetSnapshot(
    val signedIn: Boolean = false,
    /** The next orders to arrive, soonest first. */
    val arrivals: List<WidgetArrival> = emptyList(),
    /** Arrived and not read: the pile. */
    val unread: Int = 0,
    /** Of [unread], the volumes being read. */
    val reading: Int = 0,
    /** When the longest-waiting unread volume (not one being read) arrived; null if none says. */
    val oldestUnreadSince: String? = null,
) {
    /** The arrivals still ahead on [today]: an order whose release date has passed drops off. */
    fun arrivalsOn(today: String): List<WidgetArrival> =
        arrivals.filter { it.status != OrderStatus.ORDERED.name || it.date >= today }

    /** How long the oldest unread volume has waited by [today]. */
    fun oldestWaitingDays(today: String): Int? =
        oldestUnreadSince?.let { daysBetween(it, today) }?.coerceAtLeast(0)

    fun encode(): String = snapshotJson.encodeToString(this)

    companion object {
        val SignedOut = WidgetSnapshot()

        /** A snapshot from [text], or the signed-out one when there is none or it can't be read. */
        fun decodeOrSignedOut(text: String?): WidgetSnapshot =
            text?.let { runCatching { snapshotJson.decodeFromString<WidgetSnapshot>(it) }.getOrNull() }
                ?: SignedOut
    }
}

@Serializable
data class WidgetArrival(
    val id: String,
    val title: String,
    val volume: String,
    /** The date it is expected, released or delayed to. */
    val date: String,
    /** [OrderStatus] name: ORDERED (a release), SHIPPED or DELAYED. */
    val status: String,
)

/**
 * How many arrivals the snapshot keeps. More than anyone scrolls through on a widget; the bound is
 * there because a lazy list on Android 12+ sends all its rows to the launcher in one Binder
 * transaction (about 1 MB), and fifty rows of text are a small fraction of that.
 */
private const val MAX_ARRIVALS = 50

/**
 * The snapshot's JSON is also read by the iOS widget, in Swift, so it is a contract: every field is
 * always written, defaults included, and a test pins the exact shape. Unknown keys are ignored so a
 * newer app's file still reads in an older widget, and the other way round.
 */
private val snapshotJson =
    Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

/** The widgets' view of [this] cached list of orders on [today], for a signed-in user. */
fun List<Order>.toWidgetSnapshot(today: String): WidgetSnapshot {
    val active = filter { it.status != OrderStatus.CANCELLED }
    val pile = active.filter { it.status == OrderStatus.RECEIVED && it.readState != ReadState.READ }
    return WidgetSnapshot(
        signedIn = true,
        arrivals =
            active
                .mapNotNull { order -> arrivalDate(order, today)?.let { order to it } }
                .sortedWith(compareBy<Pair<Order, String>> { it.second }.thenBy { it.first.title.lowercase() })
                .take(MAX_ARRIVALS)
                .map { (order, date) ->
                    WidgetArrival(
                        id = order.id,
                        title = order.title,
                        volume = order.volume,
                        date = date,
                        status = order.status.name,
                    )
                },
        unread = pile.size,
        reading = pile.count { it.readState == ReadState.READING },
        oldestUnreadSince =
            pile
                .filter { it.readState == ReadState.WANT && it.receivedDate.isNotBlank() }
                .minOfOrNull { it.receivedDate },
    )
}
