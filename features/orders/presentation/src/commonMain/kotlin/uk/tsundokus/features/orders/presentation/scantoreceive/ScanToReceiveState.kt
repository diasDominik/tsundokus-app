package uk.tsundokus.features.orders.presentation.scantoreceive

import uk.tsundokus.features.orders.domain.models.Order

data class ScanToReceiveState(
    val manualIsbn: String = "",
    val isManualIsbnInvalid: Boolean = false,
    /**
     * The ISBN handled last. The camera ignores it, since the book is usually still in view when
     * it re-arms, and scanning it again would only report it as already received.
     */
    val lastIsbn: String? = null,
    /** What the last scan did, shown under the viewfinder. */
    val outcome: ScanOutcome? = null,
    /** Set while the user has to say which order a scan belongs to. */
    val picking: OrderPick? = null,
    val isBusy: Boolean = false,
) {
    /** The camera is off while a choice is pending or a write is on its way, so it cannot race either. */
    val isCameraActive: Boolean get() = picking == null && !isBusy
}

sealed interface ScanOutcome {
    /** [order] was marked received; [previous] is how it was, for undo. */
    data class Received(
        val order: Order,
        val previous: Order,
    ) : ScanOutcome

    data class Undone(val order: Order) : ScanOutcome

    /** Every order with the scanned ISBN was already received or cancelled; [order] is the latest. */
    data class AlreadyReceived(val order: Order) : ScanOutcome
}

/** A scan that needs the user to pick which of [orders] it was. */
data class OrderPick(
    val isbn: String,
    val reason: Reason,
    val orders: List<Order>,
) {
    enum class Reason {
        /** Several open orders carry this ISBN. */
        DUPLICATE,

        /** No order carries it yet; [orders] are the open ones with no ISBN. */
        UNKNOWN,
    }
}
