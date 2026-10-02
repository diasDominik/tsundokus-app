package uk.tsundokus.features.orders.presentation.stats

import uk.tsundokus.features.orders.domain.stats.StatsPeriod

sealed interface StatsAction {
    data class OnPeriodSelected(val period: StatsPeriod) : StatsAction
}
