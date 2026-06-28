package uk.tsundokus.features.orders.presentation.reportdelay

import uk.tsundokus.core.presentation.util.UiText

sealed interface ReportDelayEvent {
    data class Saved(val message: UiText) : ReportDelayEvent

    data class ShowError(val message: UiText) : ReportDelayEvent
}
