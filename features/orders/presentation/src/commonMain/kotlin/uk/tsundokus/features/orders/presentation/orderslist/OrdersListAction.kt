package uk.tsundokus.features.orders.presentation.orderslist

import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.selection.SelectionChange

sealed interface OrdersListAction {
    data class OnSearchQueryChange(val query: String) : OrdersListAction

    /** Picking the sort already in use flips its direction; picking a new one adopts its default. */
    data class OnSortSelected(val sort: OrderSort) : OrdersListAction

    data object OnRefresh : OrdersListAction

    data class OnStatusFilterSelected(val status: OrderStatus?) : OrdersListAction

    data class OnOrderSelected(val orderId: String) : OrdersListAction

    // Picking orders to act on together. "Selected" above is the order shown in the detail pane;
    // these are the ones picked, which is why they say so.
    data object OnStartPicking : OrdersListAction

    /** Picks or unpicks an order; a long press starts picking with it. */
    data class OnTogglePicked(val orderId: String) : OrdersListAction

    data object OnPickAllShown : OrdersListAction

    data object OnStopPicking : OrdersListAction

    data class OnChangePicked(val change: SelectionChange) : OrdersListAction

    data object OnDeletePicked : OrdersListAction

    data object OnUndo : OrdersListAction
}
