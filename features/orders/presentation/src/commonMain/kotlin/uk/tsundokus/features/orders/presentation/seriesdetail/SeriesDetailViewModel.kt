package uk.tsundokus.features.orders.presentation.seriesdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.series.Series
import uk.tsundokus.features.orders.domain.series.toSeries
import kotlin.time.Duration.Companion.seconds

data class SeriesDetailState(
    val isLoading: Boolean = true,
    /** Null once loaded means the series is gone — its last order was deleted or retitled. */
    val series: Series? = null,
)

@KoinViewModel
class SeriesDetailViewModel(
    @InjectedParam private val seriesKey: String,
    orderRepository: OrderRepository,
) : ViewModel() {
    val state: StateFlow<SeriesDetailState> =
        orderRepository
            .getOrders()
            .map { orders ->
                SeriesDetailState(
                    isLoading = false,
                    series =
                        orders.toSeries().firstOrNull {
                            it.key ==
                                seriesKey
                        },
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = SeriesDetailState(),
            )
}
