package uk.tsundokus.features.settings.presentation.passkeys

import uk.tsundokus.features.settings.domain.passkeys.Passkey

data class PasskeysState(
    val passkeys: List<Passkey> = emptyList(),
    val isLoading: Boolean = true,
    /** Set from the moment the add button is pressed until the ceremony resolves either way. */
    val isEnrolling: Boolean = false,
    /** The passkey the confirmation dialog is asking about, if any. */
    val removing: Passkey? = null,
)
