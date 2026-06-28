package uk.tsundokus.features.orders.presentation.addeditorder

import uk.tsundokus.core.presentation.util.UiText

sealed interface AddEditOrderEvent {
    data class Saved(val message: UiText) : AddEditOrderEvent

    data class Deleted(val message: UiText) : AddEditOrderEvent

    data class ShowError(val message: UiText) : AddEditOrderEvent
}
