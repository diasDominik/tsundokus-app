package uk.tsundokus.features.orders.presentation.orderslist

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus

@Stable
data class OrdersListState(
    val isLoading: Boolean = true,
    val allOrders: List<Order> = emptyList(),
    val displayed: List<Order> = emptyList(),
    val searchQuery: String = "",
    val sort: OrderSort = OrderSort.RECENT,
    val statusFilter: OrderStatus? = null,
    val nextArrival: Order? = null,
    val grouped: Map<OrderStatus, List<Order>> = emptyMap(),
    val selectedOrderId: String? = null,
    val counts: Map<OrderStatus?, Int> = emptyMap(),
) {
    val selectedOrder: Order?
        get() = allOrders.firstOrNull { it.id == selectedOrderId }
}
