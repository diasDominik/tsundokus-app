package uk.tsundokus.features.orders.presentation.orderslist

import uk.tsundokus.core.presentation.util.UiText

sealed interface OrdersListEvent {
    data class ShowMessage(val message: UiText) : OrdersListEvent

    /** A message about a change to picked orders, with an Undo button. */
    data class ShowUndoableMessage(val message: UiText) : OrdersListEvent
}
