package uk.tsundokus.features.settings.presentation.passkeys

import uk.tsundokus.core.presentation.util.UiText

sealed interface PasskeysEvent {
    /**
     * Create a passkey with [optionsJson] and send the result back.
     *
     * The platform authenticator is reached through a composable-scoped client, so the ceremony
     * belongs to the screen; the ViewModel only asks for it and judges what comes back.
     */
    data class RunEnrolmentCeremony(val optionsJson: String) : PasskeysEvent

    data class ShowMessage(val message: UiText) : PasskeysEvent
}
