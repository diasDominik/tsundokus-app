package uk.tsundokus.features.orders.presentation.serieslist

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
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.series.Series
import uk.tsundokus.features.orders.domain.series.toSeries
import kotlin.time.Duration.Companion.seconds

/** The collection by series, worked out from the cached orders — offline, no request of its own. */
@KoinViewModel
class SeriesListViewModel(
    private val orderRepository: OrderRepository,
) : ViewModel() {
    private val eventChannel = Channel<SeriesListEvent>()
    val events = eventChannel.receiveAsFlow()

    private val searchQuery = MutableStateFlow("")
    private val sort = MutableStateFlow(SeriesSort.RECENT)
    private val selectedKey = MutableStateFlow<String?>(null)

    val state: StateFlow<SeriesListState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            searchQuery,
            sort,
            selectedKey,
        ) { orders, query, sort, selected ->
            SeriesListState(
                isLoading = false,
                searchQuery = query,
                sort = sort,
                series = orders.toSeries().filter { it.matchesQuery(query) }.sortedWith(comparatorFor(sort)),
                selectedKey = selected,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SeriesListState(),
        )

    init {
        viewModelScope.launch {
            orderRepository.fetchOrders().onFailure { error ->
                eventChannel.send(SeriesListEvent.ShowMessage(error.toUiText()))
            }
        }
    }

    fun onAction(action: SeriesListAction) {
        when (action) {
            is SeriesListAction.OnSearchQueryChange -> searchQuery.value = action.query
            is SeriesListAction.OnSortSelected -> sort.value = action.sort
            is SeriesListAction.OnSeriesSelected -> selectedKey.value = action.key
        }
    }
}

private fun comparatorFor(sort: SeriesSort): Comparator<Series> =
    when (sort) {
        SeriesSort.RECENT -> {
            compareByDescending(Series::lastActivity)
        }

        SeriesSort.TITLE -> {
            compareBy(String.CASE_INSENSITIVE_ORDER, Series::title)
        }

        SeriesSort.MISSING -> {
            compareByDescending<Series> { it.missing.size }.thenByDescending(
                Series::lastActivity,
            )
        }
    }
