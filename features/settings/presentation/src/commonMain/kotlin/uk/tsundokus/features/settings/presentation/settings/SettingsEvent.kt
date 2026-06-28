package uk.tsundokus.features.settings.presentation.settings

import uk.tsundokus.core.presentation.util.UiText

sealed interface SettingsEvent {
    data object SignedOut : SettingsEvent

    data class ShowMessage(val message: UiText) : SettingsEvent
}
