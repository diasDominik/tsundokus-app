package uk.tsundokus.features.orders.presentation.orderslist

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.SortDirection

@Stable
data class OrdersListState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val allOrders: List<Order> = emptyList(),
    val displayed: List<Order> = emptyList(),
    val searchQuery: String = "",
    val sort: OrderSort = OrderSort.RECENT,
    val sortDirection: SortDirection = OrderSort.RECENT.defaultDirection,
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

    /**
     * Whether the list is currently narrowed by the user. An empty list means something different
     * in each case — nothing logged yet, versus nothing matching — so the empty state reads this
     * rather than assuming the first-run case.
     */
    val isFiltered: Boolean
        get() = searchQuery.isNotBlank() || statusFilter != null
}
