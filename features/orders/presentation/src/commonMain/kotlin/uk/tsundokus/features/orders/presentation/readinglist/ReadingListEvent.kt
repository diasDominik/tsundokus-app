package uk.tsundokus.features.orders.presentation.readinglist

import uk.tsundokus.core.presentation.util.UiText

sealed interface ReadingListEvent {
    data class ShowMessage(val message: UiText) : ReadingListEvent
}
