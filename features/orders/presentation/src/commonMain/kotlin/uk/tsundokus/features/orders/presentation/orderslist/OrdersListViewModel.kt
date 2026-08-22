package uk.tsundokus.features.orders.presentation.orderslist

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
import uk.tsundokus.core.domain.sync.LastServerContactStore
import uk.tsundokus.core.domain.sync.PendingWrites
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.presentation.components.arrivalDate
import uk.tsundokus.features.orders.presentation.components.todayIso
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class OrdersListViewModel(
    private val orderRepository: OrderRepository,
    pendingWrites: PendingWrites,
    lastServerContactStore: LastServerContactStore,
) : ViewModel() {
    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(OrderSort.RECENT)
    private val statusFilter = MutableStateFlow<OrderStatus?>(null)
    private val selectedOrderId = MutableStateFlow<String?>(null)

    private val eventChannel = Channel<OrdersListEvent>()
    val events = eventChannel.receiveAsFlow()

    private val syncStatus =
        combine(pendingWrites.observeCount(), lastServerContactStore.lastContactAt) { pending, lastContact ->
            SyncStatus(pendingCount = pending, lastSyncedAt = lastContact)
        }

    val state: StateFlow<OrdersListState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            searchQuery,
            sort,
            statusFilter,
            selectedOrderId,
        ) { orders, query, sortOrder, filter, selected ->
            buildState(orders, query, sortOrder, filter, selected)
        }.combine(syncStatus) { state, sync ->
            state.copy(pendingSyncCount = sync.pendingCount, lastSyncedAt = sync.lastSyncedAt)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = OrdersListState(),
        )

    init {
        viewModelScope.launch {
            orderRepository.fetchOrders().onFailure { error ->
                eventChannel.send(OrdersListEvent.ShowMessage(error.toUiText()))
            }
        }
    }

    fun onAction(action: OrdersListAction) {
        when (action) {
            is OrdersListAction.OnSearchQueryChange -> searchQuery.value = action.query
            OrdersListAction.OnToggleSort -> sort.value = sort.value.next()
            is OrdersListAction.OnStatusFilterSelected -> statusFilter.value = action.status
            is OrdersListAction.OnOrderSelected -> selectedOrderId.value = action.orderId
        }
    }
}

private data class SyncStatus(
    val pendingCount: Int,
    val lastSyncedAt: Long?,
)

private fun buildState(
    orders: List<Order>,
    query: String,
    sortOrder: OrderSort,
    filter: OrderStatus?,
    selected: String?,
): OrdersListState {
    val today = todayIso()
    val searched = orders.filter { it.matchesQuery(query) }
    val counts: Map<OrderStatus?, Int> =
        buildMap {
            put(null, searched.size)
            OrderStatus.entries.forEach { status -> put(status, searched.count { it.status == status }) }
        }
    val filtered = if (filter == null) searched else searched.filter { it.status == filter }
    val sorted = filtered.sortedWith(sortOrder.comparator())
    val grouped: Map<OrderStatus, List<Order>> =
        if (filter == null) {
            OrderStatus.groupOrder
                .associateWith { status -> sorted.filter { it.status == status } }
                .filterValues { it.isNotEmpty() }
        } else {
            mapOf(filter to sorted)
        }
    return OrdersListState(
        isLoading = false,
        allOrders = orders,
        displayed = sorted,
        searchQuery = query,
        sort = sortOrder,
        statusFilter = filter,
        nextArrival = orders.nextArrival(today),
        grouped = grouped,
        selectedOrderId = selected ?: sorted.firstOrNull()?.id,
        counts = counts,
    )
}

private fun Order.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim()
    return title.contains(needle, ignoreCase = true) ||
        author.contains(needle, ignoreCase = true) ||
        publisher.contains(needle, ignoreCase = true)
}

private fun OrderSort.comparator(): Comparator<Order> =
    when (this) {
        OrderSort.RECENT -> compareByDescending<Order> { it.createdAt }
        OrderSort.RELEASE -> compareBy<Order>({ it.releaseDate.isBlank() }, { it.releaseDate })
        OrderSort.TITLE -> compareBy<Order> { it.title.lowercase() }
        OrderSort.PRICE -> compareByDescending<Order> { it.price }
    }

private fun List<Order>.nextArrival(today: String): Order? =
    mapNotNull { order -> arrivalDate(order, today)?.let { order to it } }
        .minByOrNull { it.second }
        ?.first
