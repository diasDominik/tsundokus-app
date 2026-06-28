package uk.tsundokus.features.authentication.presentation.register

import uk.tsundokus.core.presentation.util.UiText

sealed interface RegisterEvent {
    data class RegistrationSuccess(val email: String) : RegisterEvent

    data class RegistrationError(val message: UiText) : RegisterEvent
}
