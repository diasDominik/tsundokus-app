package uk.tsundokus.features.orders.presentation.scantoreceive

import uk.tsundokus.core.presentation.util.UiText

sealed interface ScanToReceiveEvent {
    data class AddOrder(val isbn: String) : ScanToReceiveEvent

    data class ShowError(val message: UiText) : ScanToReceiveEvent
}
