package uk.tsundokus.features.orders.presentation.components

import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_sort_price
import tsundokuapp.features.orders.presentation.generated.resources.order_sort_recent
import tsundokuapp.features.orders.presentation.generated.resources.order_sort_release
import tsundokuapp.features.orders.presentation.generated.resources.order_sort_title
import tsundokuapp.features.orders.presentation.generated.resources.order_status_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.order_status_delayed
import tsundokuapp.features.orders.presentation.generated.resources.order_status_ordered
import tsundokuapp.features.orders.presentation.generated.resources.order_status_received
import tsundokuapp.features.orders.presentation.generated.resources.order_status_shipped
import tsundokuapp.features.orders.presentation.generated.resources.read_state_read
import tsundokuapp.features.orders.presentation.generated.resources.read_state_read_full
import tsundokuapp.features.orders.presentation.generated.resources.read_state_reading
import tsundokuapp.features.orders.presentation.generated.resources.read_state_reading_full
import tsundokuapp.features.orders.presentation.generated.resources.read_state_want
import tsundokuapp.features.orders.presentation.generated.resources.read_state_want_full
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/**
 * Display copy for the domain enums. The enums themselves carry no user-facing text, so this is
 * the single place a status or read state is named — including for the ViewModels, which pair
 * these with [uk.tsundokus.core.presentation.util.UiText.Resource].
 */

val OrderStatus.labelRes: StringResource
    get() =
        when (this) {
            OrderStatus.ORDERED -> Res.string.order_status_ordered
            OrderStatus.SHIPPED -> Res.string.order_status_shipped
            OrderStatus.DELAYED -> Res.string.order_status_delayed
            OrderStatus.RECEIVED -> Res.string.order_status_received
            OrderStatus.CANCELLED -> Res.string.order_status_cancelled
        }

/** Short form used on badges and pills. */
val ReadState.labelRes: StringResource
    get() =
        when (this) {
            ReadState.WANT -> Res.string.read_state_want
            ReadState.READING -> Res.string.read_state_reading
            ReadState.READ -> Res.string.read_state_read
        }

/** Long form used on section headers and the read-state picker. */
val ReadState.fullLabelRes: StringResource
    get() =
        when (this) {
            ReadState.WANT -> Res.string.read_state_want_full
            ReadState.READING -> Res.string.read_state_reading_full
            ReadState.READ -> Res.string.read_state_read_full
        }

val OrderSort.labelRes: StringResource
    get() =
        when (this) {
            OrderSort.RECENT -> Res.string.order_sort_recent
            OrderSort.RELEASE -> Res.string.order_sort_release
            OrderSort.TITLE -> Res.string.order_sort_title
            OrderSort.PRICE -> Res.string.order_sort_price
        }
