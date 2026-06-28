package uk.tsundokus.features.orders.presentation.components

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.math.abs
import kotlin.math.round
import kotlin.time.Clock

/**
 * Date / price formatting that avoids a datetime dependency. Dates are ISO `yyyy-MM-dd`
 * strings, which compare lexicographically in chronological order, so `<` / `>` work directly.
 */

private val MONTHS =
    arrayOf(
        "Jan",
        "Feb",
        "Mar",
        "Apr",
        "May",
        "Jun",
        "Jul",
        "Aug",
        "Sep",
        "Oct",
        "Nov",
        "Dec",
    )

/** Formats an ISO `yyyy-MM-dd` string as `d MMM yyyy` (e.g. `7 Mar 2026`). Blank -> "". */
fun fmtDate(iso: String): String {
    if (iso.isBlank()) return ""
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    if (month !in 1..12) return iso
    return "$day ${MONTHS[month - 1]} ${parts[0]}"
}

/** Today as an ISO `yyyy-MM-dd` string (UTC), used for "releases/expected" comparisons. */
fun todayIso(): String {
    val days =
        Clock.System
            .now()
            .toEpochMilliseconds()
            .floorDiv(86_400_000L)
    return isoFromEpochDay(days)
}

/** Current epoch milliseconds — the [uk.tsundokus.features.orders.domain.models.Order] RECENT sort key. */
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

// Howard Hinnant's civil-from-days algorithm (days since 1970-01-01 -> y/m/d).
private fun isoFromEpochDay(epochDay: Long): String {
    val z = epochDay + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = if (month <= 2) y + 1 else y
    return "${pad4(year)}-${pad2(month)}-${pad2(day)}"
}

private fun pad2(value: Int): String = value.toString().padStart(2, '0')

private fun pad4(value: Long): String = value.toString().padStart(4, '0')

/** Currency symbol + amount with two decimals, e.g. `€19.99`. */
fun priceLabel(order: Order): String = order.currency.symbol + formatAmount(order.price)

private fun formatAmount(value: Double): String {
    val cents = round(value * 100).toLong()
    val sign = if (cents < 0) "-" else ""
    val magnitude = abs(cents)
    val whole = magnitude / 100
    val fraction = (magnitude % 100).toString().padStart(2, '0')
    return "$sign$whole.$fraction"
}

/** Status-aware secondary label for a row (mirrors the design JS). */
fun dateLabel(
    order: Order,
    today: String,
): String =
    when (order.status) {
        OrderStatus.SHIPPED -> {
            if (order.eta.isNotBlank()) "ETA ${fmtDate(order.eta)}" else "In transit"
        }

        OrderStatus.DELAYED -> {
            if (order.delayedTo.isNotBlank()) "Delayed → ${fmtDate(order.delayedTo)}" else "Delayed"
        }

        OrderStatus.RECEIVED -> {
            if (order.receivedDate.isNotBlank()) "Got it ${fmtDate(order.receivedDate)}" else "Received"
        }

        OrderStatus.CANCELLED -> {
            "Cancelled"
        }

        OrderStatus.ORDERED -> {
            if (order.releaseDate.isNotBlank() && order.releaseDate > today) {
                "Releases ${fmtDate(order.releaseDate)}"
            } else {
                "Ordered" + if (order.orderDate.isNotBlank()) " ${fmtDate(order.orderDate)}" else ""
            }
        }
    }

/**
 * The "expected arrival" date for an order, or null when it has none. Used both for ranking the
 * next-arrival hero and for the hero's subtitle.
 */
fun arrivalDate(
    order: Order,
    today: String,
): String? =
    when (order.status) {
        OrderStatus.SHIPPED -> {
            order.eta.ifBlank { null }
        }

        OrderStatus.DELAYED -> {
            order.delayedTo.ifBlank { null }
        }

        OrderStatus.ORDERED -> {
            if (order.releaseDate.isNotBlank() && order.releaseDate > today) order.releaseDate else null
        }

        else -> {
            null
        }
    }
