package uk.tsundokus.features.orders.presentation.stats

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.stats_period_12_months
import tsundokuapp.features.orders.presentation.generated.resources.stats_period_all_time
import tsundokuapp.features.orders.presentation.generated.resources.stats_period_this_year
import uk.tsundokus.features.orders.domain.stats.CollectionStats
import uk.tsundokus.features.orders.domain.stats.StatsPeriod

@Stable
data class StatsState(
    val isLoading: Boolean = true,
    val period: StatsPeriod = StatsPeriod.LAST_12_MONTHS,
    /** Null until the orders have been read once. */
    val stats: CollectionStats? = null,
)

val StatsPeriod.labelRes: StringResource
    get() =
        when (this) {
            StatsPeriod.LAST_12_MONTHS -> Res.string.stats_period_12_months
            StatsPeriod.THIS_YEAR -> Res.string.stats_period_this_year
            StatsPeriod.ALL_TIME -> Res.string.stats_period_all_time
        }
