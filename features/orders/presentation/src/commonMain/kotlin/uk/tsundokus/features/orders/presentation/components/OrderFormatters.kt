package uk.tsundokus.features.orders.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_row_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.order_row_delayed
import tsundokuapp.features.orders.presentation.generated.resources.order_row_delayed_to
import tsundokuapp.features.orders.presentation.generated.resources.order_row_eta
import tsundokuapp.features.orders.presentation.generated.resources.order_row_in_transit
import tsundokuapp.features.orders.presentation.generated.resources.order_row_ordered
import tsundokuapp.features.orders.presentation.generated.resources.order_row_ordered_on
import tsundokuapp.features.orders.presentation.generated.resources.order_row_received
import tsundokuapp.features.orders.presentation.generated.resources.order_row_received_on
import tsundokuapp.features.orders.presentation.generated.resources.order_row_releases
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
fun todayIso(): String = isoFromEpochMillis(Clock.System.now().toEpochMilliseconds())

/**
 * ISO `yyyy-MM-dd` for a UTC epoch-millis instant — the form the Material date picker hands back.
 */
fun isoFromEpochMillis(millis: Long): String = isoFromEpochDay(millis.floorDiv(MILLIS_PER_DAY))

/**
 * UTC epoch millis for midnight on an ISO `yyyy-MM-dd` date, or null when the string is blank or
 * malformed. Used to seed the Material date picker from a stored date.
 */
fun epochMillisFromIso(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toLongOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return epochDayFromIso(year, month, day) * MILLIS_PER_DAY
}

/** Current epoch milliseconds — the [uk.tsundokus.features.orders.domain.models.Order] RECENT sort key. */
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

private const val MILLIS_PER_DAY = 86_400_000L

// Howard Hinnant's days-from-civil algorithm (y/m/d -> days since 1970-01-01).
private fun epochDayFromIso(
    year: Long,
    month: Int,
    day: Int,
): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}

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
@Composable
fun dateLabel(
    order: Order,
    today: String,
): String =
    when (order.status) {
        OrderStatus.SHIPPED -> {
            if (order.eta.isNotBlank()) {
                stringResource(Res.string.order_row_eta, fmtDate(order.eta))
            } else {
                stringResource(Res.string.order_row_in_transit)
            }
        }

        OrderStatus.DELAYED -> {
            if (order.delayedTo.isNotBlank()) {
                stringResource(Res.string.order_row_delayed_to, fmtDate(order.delayedTo))
            } else {
                stringResource(Res.string.order_row_delayed)
            }
        }

        OrderStatus.RECEIVED -> {
            if (order.receivedDate.isNotBlank()) {
                stringResource(Res.string.order_row_received_on, fmtDate(order.receivedDate))
            } else {
                stringResource(Res.string.order_row_received)
            }
        }

        OrderStatus.CANCELLED -> {
            stringResource(Res.string.order_row_cancelled)
        }

        OrderStatus.ORDERED -> {
            if (order.releaseDate.isNotBlank() && order.releaseDate > today) {
                stringResource(Res.string.order_row_releases, fmtDate(order.releaseDate))
            } else if (order.orderDate.isNotBlank()) {
                stringResource(Res.string.order_row_ordered_on, fmtDate(order.orderDate))
            } else {
                stringResource(Res.string.order_row_ordered)
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
