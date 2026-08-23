package uk.tsundokus.features.orders.presentation.orderslist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
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
import uk.tsundokus.features.orders.domain.models.SortDirection
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.preferences.OrderSortPreference
import uk.tsundokus.features.orders.domain.preferences.OrdersPreferences
import uk.tsundokus.features.orders.presentation.components.arrivalDate
import uk.tsundokus.features.orders.presentation.components.todayIso
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class OrdersListViewModel(
    private val orderRepository: OrderRepository,
    private val ordersPreferences: OrdersPreferences,
    pendingWrites: PendingWrites,
    lastServerContactStore: LastServerContactStore,
) : ViewModel() {
    // Search and the status filter are transient narrowings: they live and die with the screen.
    // Sort is a durable preference and is read from (and written back to) OrdersPreferences.
    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow<OrderStatus?>(null)
    private val selectedOrderId = MutableStateFlow<String?>(null)
    private val isRefreshing = MutableStateFlow(false)

    private val eventChannel = Channel<OrdersListEvent>()
    val events = eventChannel.receiveAsFlow()

    private val syncStatus =
        combine(pendingWrites.observeCount(), lastServerContactStore.lastContactAt) { pending, lastContact ->
            SyncStatus(pendingCount = pending, lastSyncedAt = lastContact)
        }

    private val narrowing =
        combine(searchQuery, statusFilter, selectedOrderId, isRefreshing) { query, filter, selected, refreshing ->
            Narrowing(query = query, filter = filter, selectedOrderId = selected, isRefreshing = refreshing)
        }

    val state: StateFlow<OrdersListState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            ordersPreferences.sort(),
            narrowing,
        ) { orders, sortPreference, current ->
            buildState(orders, sortPreference, current)
        }.combine(syncStatus) { state, sync ->
            state.copy(pendingSyncCount = sync.pendingCount, lastSyncedAt = sync.lastSyncedAt)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = OrdersListState(),
        )

    init {
        refresh()
    }

    fun onAction(action: OrdersListAction) {
        when (action) {
            is OrdersListAction.OnSearchQueryChange -> searchQuery.value = action.query
            is OrdersListAction.OnSortSelected -> onSortSelected(action.sort)
            OrdersListAction.OnRefresh -> refresh()
            is OrdersListAction.OnStatusFilterSelected -> statusFilter.value = action.status
            is OrdersListAction.OnOrderSelected -> selectedOrderId.value = action.orderId
        }
    }

    /** Re-picking the current sort reverses it; picking a different one starts at its default. */
    private fun onSortSelected(sort: OrderSort) {
        viewModelScope.launch {
            val current = ordersPreferences.sort().first()
            val next =
                if (current.sort == sort) {
                    current.copy(direction = current.direction.flipped())
                } else {
                    OrderSortPreference(sort = sort, direction = sort.defaultDirection)
                }
            ordersPreferences.setSort(next)
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            isRefreshing.value = true
            orderRepository.fetchOrders().onFailure { error ->
                eventChannel.send(OrdersListEvent.ShowMessage(error.toUiText()))
            }
            isRefreshing.value = false
        }
    }
}

private data class Narrowing(
    val query: String,
    val filter: OrderStatus?,
    val selectedOrderId: String?,
    val isRefreshing: Boolean,
)

private data class SyncStatus(
    val pendingCount: Int,
    val lastSyncedAt: Long?,
)

private fun buildState(
    orders: List<Order>,
    sortPreference: OrderSortPreference,
    current: Narrowing,
): OrdersListState {
    val today = todayIso()
    val searched = orders.filter { it.matchesQuery(current.query) }
    val counts: Map<OrderStatus?, Int> =
        buildMap {
            put(null, searched.size)
            OrderStatus.entries.forEach { status -> put(status, searched.count { it.status == status }) }
        }
    val filter = current.filter
    val filtered = if (filter == null) searched else searched.filter { it.status == filter }
    val sorted = filtered.sortedWith(comparatorFor(sortPreference))
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
        isRefreshing = current.isRefreshing,
        allOrders = orders,
        displayed = sorted,
        searchQuery = current.query,
        sort = sortPreference.sort,
        sortDirection = sortPreference.direction,
        statusFilter = filter,
        nextArrival = orders.nextArrival(today),
        grouped = grouped,
        selectedOrderId = current.selectedOrderId ?: sorted.firstOrNull()?.id,
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

private fun comparatorFor(preference: OrderSortPreference): Comparator<Order> {
    val ascending: Comparator<Order> =
        when (preference.sort) {
            OrderSort.RECENT -> compareBy { it.createdAt }
            OrderSort.RELEASE -> compareBy { it.releaseDate }
            OrderSort.TITLE -> compareBy { it.title.lowercase() }
            OrderSort.PRICE -> compareBy { it.price }
        }
    val byValue =
        if (preference.direction == SortDirection.ASCENDING) ascending else ascending.reversed()
    // Orders with no value for the key sink to the bottom in *both* directions: an unknown release
    // date is not the earliest or the latest, it is simply unknown. Reversing the whole comparator
    // instead would float those to the top.
    val unsetLast = compareBy<Order> { preference.sort.isUnsetFor(it) }
    return unsetLast.then(byValue)
}

private fun OrderSort.isUnsetFor(order: Order): Boolean =
    when (this) {
        OrderSort.RELEASE -> order.releaseDate.isBlank()
        OrderSort.RECENT, OrderSort.TITLE, OrderSort.PRICE -> false
    }

private fun List<Order>.nextArrival(today: String): Order? =
    mapNotNull { order -> arrivalDate(order, today)?.let { order to it } }
        .minByOrNull { it.second }
        ?.first
