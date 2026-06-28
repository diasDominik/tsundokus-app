package uk.tsundokus.features.settings.presentation.changeemail

import uk.tsundokus.core.presentation.util.UiText

data class ChangeEmailState(
    val isSubmitting: Boolean = false,
)

sealed interface ChangeEmailEvent {
    data object Saved : ChangeEmailEvent

    data class Error(val message: UiText) : ChangeEmailEvent
}
