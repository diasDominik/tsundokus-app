package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_action_mark_as_read
import tsundokuapp.features.orders.presentation.generated.resources.order_action_mark_received
import tsundokuapp.features.orders.presentation.generated.resources.order_action_mark_shipped
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.presentation.components.TimelineNode

/** The primary call-to-action shown on the detail screen, derived from the order's status. */
enum class PrimaryAction {
    MARK_SHIPPED,
    MARK_RECEIVED,
    MARK_AS_READ,
}

@Stable
data class OrderDetailState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val timeline: List<TimelineNode> = emptyList(),
    val primaryAction: PrimaryAction? = null,
)

val PrimaryAction.labelRes: StringResource
    get() =
        when (this) {
            PrimaryAction.MARK_SHIPPED -> Res.string.order_action_mark_shipped
            PrimaryAction.MARK_RECEIVED -> Res.string.order_action_mark_received
            PrimaryAction.MARK_AS_READ -> Res.string.order_action_mark_as_read
        }
