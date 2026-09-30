package uk.tsundokus.features.orders.presentation.serieslist

import uk.tsundokus.core.presentation.util.UiText

sealed interface SeriesListEvent {
    data class ShowMessage(val message: UiText) : SeriesListEvent
}
