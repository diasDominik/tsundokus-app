package uk.tsundokus.features.orders.presentation.orderslist

import uk.tsundokus.core.presentation.util.UiText

sealed interface OrdersListEvent {
    data class ShowMessage(val message: UiText) : OrdersListEvent
}
