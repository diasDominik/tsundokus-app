package uk.tsundokus.features.orders.presentation.scantoreceive

sealed interface ScanToReceiveAction {
    /** The camera read an ISBN, already normalised to ISBN-13 digits. */
    data class OnIsbnScanned(val isbn: String) : ScanToReceiveAction

    data class OnManualIsbnChange(val value: String) : ScanToReceiveAction

    data object OnManualIsbnSubmit : ScanToReceiveAction

    data class OnOrderPicked(val orderId: String) : ScanToReceiveAction

    data object OnPickDismissed : ScanToReceiveAction

    /** Start a new order for the ISBN nothing matched. */
    data object OnAddAsNewOrder : ScanToReceiveAction

    data object OnUndo : ScanToReceiveAction
}
