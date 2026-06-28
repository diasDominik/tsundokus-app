package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.presentation.components.TimelineNode

/** The primary call-to-action shown on the detail screen, derived from the order's status. */
enum class PrimaryAction(val label: String) {
    MARK_SHIPPED("Mark shipped"),
    MARK_RECEIVED("Mark received"),
    MARK_AS_READ("Mark as read"),
}

@Stable
data class OrderDetailState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val timeline: List<TimelineNode> = emptyList(),
    val primaryAction: PrimaryAction? = null,
)
