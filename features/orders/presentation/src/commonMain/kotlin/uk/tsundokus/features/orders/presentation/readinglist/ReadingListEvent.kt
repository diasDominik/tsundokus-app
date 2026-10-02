package uk.tsundokus.features.orders.presentation.readinglist

import uk.tsundokus.core.presentation.util.UiText

sealed interface ReadingListEvent {
    data class ShowMessage(val message: UiText) : ReadingListEvent

    /** A message about a change to picked orders, with an Undo button. */
    data class ShowUndoableMessage(val message: UiText) : ReadingListEvent
}
