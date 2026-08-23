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

    private val searchQuery = MutableStateFlow("")

    val state: StateFlow<ReadingListState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            searchQuery,
        ) { orders, query ->
            ReadingListState(
                isLoading = false,
                searchQuery = query,
                grouped = orders.filter { it.matchesQuery(query) }.groupForShelf(),
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

/** Same fields the orders list searches, so one habit works on both screens. */
private fun Order.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim()
    return title.contains(needle, ignoreCase = true) ||
        author.contains(needle, ignoreCase = true) ||
        publisher.contains(needle, ignoreCase = true)
}

private fun List<Order>.groupForShelf(): Map<ReadState, List<Order>> =
    ReadState.groupOrder
        .associateWith { readState ->
            filter { it.readState == readState && it.status != OrderStatus.CANCELLED }
        }.filterValues { it.isNotEmpty() }
