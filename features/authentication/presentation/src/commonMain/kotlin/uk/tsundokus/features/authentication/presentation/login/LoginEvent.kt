package uk.tsundokus.features.authentication.presentation.login

import uk.tsundokus.core.presentation.util.UiText

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent

    data class LoginFailure(val error: UiText) : LoginEvent
}
