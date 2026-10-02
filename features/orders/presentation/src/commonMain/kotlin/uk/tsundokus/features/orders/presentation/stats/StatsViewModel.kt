package uk.tsundokus.features.orders.presentation.stats

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
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.stats.StatsPeriod
import uk.tsundokus.features.orders.domain.stats.toCollectionStats
import kotlin.time.Duration.Companion.seconds

/** The collection in numbers, worked out from the cached orders — offline, no request of its own. */
@KoinViewModel
class StatsViewModel(
    private val orderRepository: OrderRepository,
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    private val eventChannel = Channel<StatsEvent>()
    val events = eventChannel.receiveAsFlow()

    private val period = MutableStateFlow(StatsPeriod.LAST_12_MONTHS)

    val state: StateFlow<StatsState> =
        combine(
            orderRepository.getOrders().onStart { emit(emptyList()) },
            period,
            appPreferencesRepository.currency(),
        ) { orders, period, currency ->
            StatsState(
                isLoading = false,
                period = period,
                stats = orders.toCollectionStats(todayIso(), period, currency),
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = StatsState(),
        )

    init {
        viewModelScope.launch {
            orderRepository.fetchOrders().onFailure { error ->
                eventChannel.send(StatsEvent.ShowMessage(error.toUiText()))
            }
        }
    }

    fun onAction(action: StatsAction) {
        when (action) {
            is StatsAction.OnPeriodSelected -> period.value = action.period
        }
    }
}
