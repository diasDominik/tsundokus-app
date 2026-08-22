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
    // Sync status: number of local writes not yet on the server, and the last successful sync
    // (epoch millis, null = never). Drives the header sync indicator.
    val pendingSyncCount: Int = 0,
    val lastSyncedAt: Long? = null,
) {
    val selectedOrder: Order?
        get() = allOrders.firstOrNull { it.id == selectedOrderId }
}
