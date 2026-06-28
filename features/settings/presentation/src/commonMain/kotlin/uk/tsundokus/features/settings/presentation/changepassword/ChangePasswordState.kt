package uk.tsundokus.features.settings.presentation.changepassword

import uk.tsundokus.core.presentation.util.UiText

data class ChangePasswordState(
    val isSubmitting: Boolean = false,
)

sealed interface ChangePasswordEvent {
    data object Saved : ChangePasswordEvent

    data class Error(val message: UiText) : ChangePasswordEvent
}
