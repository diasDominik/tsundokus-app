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
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.presentation.date.formatMediumDate
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.math.abs
import kotlin.math.round

// Date / price formatting. Dates are stored and compared as ISO `yyyy-MM-dd` strings (see
// uk.tsundokus.features.orders.domain.dates); only display goes through the platform formatter.

/**
 * Formats an ISO `yyyy-MM-dd` string in the viewer's locale (`7 Mar 2026`, `Mar 7, 2026`,
 * `2026年3月7日`). Blank input gives "", and an unparseable date is passed through as-is.
 */
fun fmtDate(iso: String): String {
    if (iso.isBlank()) return ""
    val parts = iso.split("-")
    if (parts.size != 3) return iso
    val year = parts[0].toIntOrNull() ?: return iso
    val month = parts[1].toIntOrNull() ?: return iso
    val day = parts[2].toIntOrNull() ?: return iso
    if (month !in 1..12 || day !in 1..31) return iso
    return formatMediumDate(year, month, day)
}

/** The order's price in its currency; see [amountLabel]. */
fun priceLabel(order: Order): String = amountLabel(order.price, order.currency)

/** [amount] with [currency]'s decimals and symbol: `19.99 €`, `$19.99`, `¥1200`. */
fun amountLabel(
    amount: Double,
    currency: AppCurrency,
): String = currency.format(formatAmount(amount, currency.decimals))

/** [value] with exactly [decimals] fraction digits, rounded; no grouping, "." as the separator. */
internal fun formatAmount(
    value: Double,
    decimals: Int,
): String {
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val minor = round(value * scale).toLong()
    val sign = if (minor < 0) "-" else ""
    val magnitude = abs(minor)
    val whole = magnitude / scale
    if (decimals == 0) return "$sign$whole"
    val fraction = (magnitude % scale).toString().padStart(decimals, '0')
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
