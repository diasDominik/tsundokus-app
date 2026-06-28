package uk.tsundokus.features.orders.presentation.orderdetail

import uk.tsundokus.core.presentation.util.UiText

sealed interface OrderDetailEvent {
    data class ShowMessage(val message: UiText) : OrderDetailEvent

    data object Deleted : OrderDetailEvent
}
