package uk.tsundokus.features.settings.presentation.deleteaccount

import uk.tsundokus.core.presentation.util.UiText

data class DeleteAccountState(
    val confirmationValid: Boolean = false,
    val isSubmitting: Boolean = false,
)

sealed interface DeleteAccountEvent {
    data object Deleted : DeleteAccountEvent

    data class Error(val message: UiText) : DeleteAccountEvent
}
