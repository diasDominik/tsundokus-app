package uk.tsundokus.features.orders.presentation.stats

import uk.tsundokus.core.presentation.util.UiText

sealed interface StatsEvent {
    data class ShowMessage(val message: UiText) : StatsEvent
}
