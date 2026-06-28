package uk.tsundokus.features.settings.presentation.editprofile

import uk.tsundokus.core.presentation.util.UiText

data class EditProfileState(
    val isSubmitting: Boolean = false,
)

sealed interface EditProfileEvent {
    data object Saved : EditProfileEvent

    data class Error(val message: UiText) : EditProfileEvent
}
