package uk.tsundokus.features.orders.presentation.orderslist

import uk.tsundokus.features.orders.domain.models.OrderStatus

sealed interface OrdersListAction {
    data class OnSearchQueryChange(val query: String) : OrdersListAction

    data object OnToggleSort : OrdersListAction

    data class OnStatusFilterSelected(val status: OrderStatus?) : OrdersListAction

    data class OnOrderSelected(val orderId: String) : OrdersListAction
}
