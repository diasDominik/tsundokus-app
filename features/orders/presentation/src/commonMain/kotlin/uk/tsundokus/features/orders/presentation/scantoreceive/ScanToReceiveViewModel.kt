package uk.tsundokus.features.orders.presentation.scantoreceive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.models.Isbn
import uk.tsundokus.features.orders.domain.models.IsbnMatch
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.order.OrderRepository

/** Scan books as they come out of the box, one after another, marking each order received. */
@KoinViewModel
class ScanToReceiveViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(ScanToReceiveState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<ScanToReceiveEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onAction(action: ScanToReceiveAction) {
        when (action) {
            is ScanToReceiveAction.OnIsbnScanned -> {
                // The camera can read the book it just handled once more before the ignore reaches
                // it; that second read is the same book, not a new scan.
                if (action.isbn != _state.value.lastIsbn) handle(action.isbn)
            }

            is ScanToReceiveAction.OnManualIsbnChange -> {
                val isbn = Isbn.sanitize(action.value)
                _state.update { it.copy(manualIsbn = isbn, isManualIsbnInvalid = false) }
                // A USB barcode scanner types all thirteen digits at once; take them without an
                // Enter. Only at thirteen: the first ten digits of an ISBN-13 can pass as an ISBN-10.
                if (isbn.length == ISBN_13_LENGTH && Isbn.normalize(isbn) != null) submitManual()
            }

            ScanToReceiveAction.OnManualIsbnSubmit -> {
                submitManual()
            }

            is ScanToReceiveAction.OnOrderPicked -> {
                val pick = _state.value.picking ?: return
                val order = pick.orders.firstOrNull { it.id == action.orderId } ?: return
                _state.update { it.copy(isBusy = true) }
                viewModelScope.launch { receive(order, pick.isbn) }
            }

            ScanToReceiveAction.OnPickDismissed -> {
                // lastIsbn stays: the unmatched book is still in front of the camera.
                _state.update { it.copy(picking = null) }
            }

            ScanToReceiveAction.OnAddAsNewOrder -> {
                val pick = _state.value.picking ?: return
                _state.update { it.copy(picking = null) }
                viewModelScope.launch { eventChannel.send(ScanToReceiveEvent.AddOrder(pick.isbn)) }
            }

            ScanToReceiveAction.OnUndo -> {
                undo()
            }
        }
    }

    /** Typed ISBNs skip the repeat guard: typing one again is deliberate. */
    private fun submitManual() {
        val isbn = Isbn.normalize(_state.value.manualIsbn)
        if (isbn == null) {
            _state.update { it.copy(isManualIsbnInvalid = true) }
            return
        }
        _state.update { it.copy(manualIsbn = "") }
        handle(isbn)
    }

    private fun handle(isbn: String) {
        _state.update { it.copy(lastIsbn = isbn, isBusy = true) }
        viewModelScope.launch {
            val orders = orderRepository.getOrders().first()
            when (val match = IsbnMatch.forIsbn(isbn, orders)) {
                is IsbnMatch.Receivable -> {
                    receive(match.order, isbn)
                }

                is IsbnMatch.Ambiguous -> {
                    _state.update {
                        it.copy(
                            isBusy = false,
                            picking = OrderPick(isbn, OrderPick.Reason.DUPLICATE, match.orders),
                        )
                    }
                }

                is IsbnMatch.NoMatch -> {
                    _state.update {
                        it.copy(
                            isBusy = false,
                            picking = OrderPick(isbn, OrderPick.Reason.UNKNOWN, match.candidates),
                        )
                    }
                }

                is IsbnMatch.AlreadyReceived -> {
                    _state.update { it.copy(isBusy = false, outcome = ScanOutcome.AlreadyReceived(match.order)) }
                }
            }
        }
    }

    /**
     * Marks [order] received, recording [isbn] on it — which is how an order from before ISBNs gets
     * one. The received date is filled in here rather than left to the server, so it shows at once.
     */
    private suspend fun receive(
        order: Order,
        isbn: String,
    ) {
        val received =
            order.copy(
                isbn = isbn,
                status = OrderStatus.RECEIVED,
                receivedDate = order.receivedDate.ifBlank { todayIso() },
            )
        orderRepository
            .updateOrder(received)
            .onSuccess { saved ->
                _state.update {
                    it.copy(
                        isBusy = false,
                        picking = null,
                        outcome = ScanOutcome.Received(saved, previous = order),
                    )
                }
            }.onFailure { error ->
                _state.update { it.copy(isBusy = false, picking = null) }
                eventChannel.send(ScanToReceiveEvent.ShowError(error.toUiText()))
            }
    }

    /**
     * Puts the order back exactly as it was, ISBN included: when the scan attached the ISBN to the
     * wrong order, undoing it must not leave the next scan of that book matching the wrong one.
     */
    private fun undo() {
        val received = _state.value.outcome as? ScanOutcome.Received ?: return
        _state.update { it.copy(isBusy = true) }
        viewModelScope.launch {
            orderRepository
                .updateOrder(received.previous)
                .onSuccess { restored ->
                    // Cleared so the same book can be scanned again straight away.
                    _state.update {
                        it.copy(
                            isBusy = false,
                            lastIsbn = null,
                            outcome = ScanOutcome.Undone(restored),
                        )
                    }
                }.onFailure { error ->
                    _state.update { it.copy(isBusy = false) }
                    eventChannel.send(ScanToReceiveEvent.ShowError(error.toUiText()))
                }
        }
    }

    private companion object {
        const val ISBN_13_LENGTH = 13
    }
}
