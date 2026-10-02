package uk.tsundokus.features.orders.presentation.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.selection_nothing_to_change
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.models.matchesQuery
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.presentation.components.fullLabelRes
import uk.tsundokus.features.orders.presentation.selection.OrderSelection
import uk.tsundokus.features.orders.presentation.selection.SelectionChange
import uk.tsundokus.features.orders.presentation.selection.doneMessage
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ReadingListViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val eventChannel = Channel<ReadingListEvent>()
    val events = eventChannel.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")
    private val selection = OrderSelection(orderRepository)

    val state: StateFlow<ReadingListState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            searchQuery,
            selection.picked,
        ) { orders, query, picked ->
            val shelf = orders.filter { it.status != OrderStatus.CANCELLED }
            ReadingListState(
                isLoading = false,
                searchQuery = query,
                grouped = shelf.filter { it.matchesQuery(query) }.groupForShelf(),
                shelf = shelf,
                picked = picked?.let { shelf.map(Order::id).filter { id -> id in picked }.toSet() },
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = ReadingListState(),
        )

    init {
        viewModelScope.launch {
            orderRepository.fetchOrders().onFailure { error ->
                eventChannel.send(ReadingListEvent.ShowMessage(error.toUiText()))
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        searchQuery.value = query
    }

    fun onStartPicking() {
        selection.start()
    }

    /** Picks or unpicks an order; a long press starts picking with it. */
    fun onTogglePicked(orderId: String) {
        selection.toggle(orderId)
    }

    fun onPickAllShown() {
        selection.selectAll(state.value.shown.map(Order::id))
    }

    fun onStopPicking() {
        selection.clear()
    }

    fun onSetPickedReadState(readState: ReadState) {
        val change = SelectionChange.Reading(readState)
        viewModelScope.launch {
            selection
                .apply(change, state.value.shelf, todayIso())
                .onSuccess { count ->
                    val event =
                        if (count == 0) {
                            ReadingListEvent.ShowMessage(UiText.Resource(Res.string.selection_nothing_to_change))
                        } else {
                            ReadingListEvent.ShowUndoableMessage(change.doneMessage(count))
                        }
                    eventChannel.send(event)
                }.onFailure { error -> eventChannel.send(ReadingListEvent.ShowMessage(error.toUiText())) }
        }
    }

    fun onUndo() {
        viewModelScope.launch {
            selection.undo().onFailure { error ->
                eventChannel.send(ReadingListEvent.ShowMessage(error.toUiText()))
            }
        }
    }

    fun onCycleReadState(orderId: String) {
        val order =
            state.value.grouped.values
                .flatten()
                .firstOrNull { it.id == orderId } ?: return
        val next = order.readState.next()
        viewModelScope.launch {
            orderRepository
                .setReadState(orderId, next)
                .onSuccess {
                    eventChannel.send(ReadingListEvent.ShowMessage(UiText.Resource(next.fullLabelRes)))
                }.onFailure { error ->
                    eventChannel.send(ReadingListEvent.ShowMessage(error.toUiText()))
                }
        }
    }
}

private fun List<Order>.groupForShelf(): Map<ReadState, List<Order>> =
    ReadState.groupOrder
        .associateWith { readState ->
            filter { it.readState == readState }
        }.filterValues { it.isNotEmpty() }
