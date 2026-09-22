package uk.tsundokus.features.authentication.presentation.login

import uk.tsundokus.core.presentation.util.UiText

sealed interface LoginEvent {
    data object LoginSuccess : LoginEvent

    /**
     * Run a passkey ceremony with [optionsJson] and send the result back.
     *
     * The platform authenticator is reached through a composable-scoped client, so the ceremony
     * itself belongs to the screen; the ViewModel only asks for it and judges what comes back.
     */
    data class RunPasskeyCeremony(val optionsJson: String) : LoginEvent

    data class LoginFailure(val error: UiText) : LoginEvent
}
