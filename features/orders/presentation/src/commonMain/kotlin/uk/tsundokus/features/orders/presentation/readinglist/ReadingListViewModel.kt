package uk.tsundokus.features.orders.presentation.readinglist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.presentation.components.fullLabelRes
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class ReadingListViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val eventChannel = Channel<ReadingListEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<ReadingListState> =
        orderRepository
            .getOrders()
            .onStart { emit(emptyList()) }
            .map { orders -> ReadingListState(isLoading = false, grouped = orders.groupForShelf()) }
            .stateIn(
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
            filter { it.readState == readState && it.status != OrderStatus.CANCELLED }
        }.filterValues { it.isNotEmpty() }
