package uk.tsundokus.features.orders.presentation.orderslist

import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus

sealed interface OrdersListAction {
    data class OnSearchQueryChange(val query: String) : OrdersListAction

    /** Picking the sort already in use flips its direction; picking a new one adopts its default. */
    data class OnSortSelected(val sort: OrderSort) : OrdersListAction

    data object OnRefresh : OrdersListAction

    data class OnStatusFilterSelected(val status: OrderStatus?) : OrdersListAction

    data class OnOrderSelected(val orderId: String) : OrdersListAction
}
