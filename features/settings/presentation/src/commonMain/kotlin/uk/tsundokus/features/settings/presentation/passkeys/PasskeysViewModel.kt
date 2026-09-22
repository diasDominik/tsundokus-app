package uk.tsundokus.features.settings.presentation.passkeys

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.error_passkey_enrolment_failed
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_added
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_removed
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.settings.domain.passkeys.Passkey
import uk.tsundokus.features.settings.domain.passkeys.PasskeyService

@KoinViewModel
class PasskeysViewModel(
    private val passkeyService: PasskeyService,
) : ViewModel() {
    private val _state = MutableStateFlow(PasskeysState())
    val state = _state.asStateFlow()

    private val eventChannel = Channel<PasskeysEvent>()
    val events = eventChannel.receiveAsFlow()

    /** The enrolment the authenticator is answering; the server pairs the response back to it. */
    private var pendingCeremonyId: String? = null

    init {
        refresh()
    }

    fun onAddPasskey() {
        if (_state.value.isEnrolling) return

        viewModelScope.launch {
            _state.update { it.copy(isEnrolling = true) }
            passkeyService
                .beginEnrolment()
                .onSuccess { ceremony ->
                    pendingCeremonyId = ceremony.ceremonyId
                    eventChannel.send(PasskeysEvent.RunEnrolmentCeremony(ceremony.optionsJson))
                }.onFailure { error ->
                    failEnrolment(error.toUiText())
                }
        }
    }

    /** What the authenticator signed, on its way to the server for verification. */
    fun onEnrolmentResponse(responseJson: String) {
        val ceremonyId = pendingCeremonyId ?: return

        viewModelScope.launch {
            passkeyService
                .finishEnrolment(ceremonyId = ceremonyId, responseJson = responseJson, label = null)
                .onSuccess { passkey ->
                    pendingCeremonyId = null
                    _state.update { it.copy(isEnrolling = false, passkeys = it.passkeys + passkey) }
                    eventChannel.send(PasskeysEvent.ShowMessage(UiText.Resource(Res.string.passkeys_added)))
                }.onFailure {
                    failEnrolment(UiText.Resource(Res.string.error_passkey_enrolment_failed))
                }
        }
    }

    /**
     * The ceremony ended at the authenticator — cancelled, or unsupported on this device. A
     * cancellation is the user's own doing, so it passes without a message.
     */
    fun onEnrolmentCeremonyFailed(message: UiText?) {
        viewModelScope.launch {
            if (message == null) {
                pendingCeremonyId = null
                _state.update { it.copy(isEnrolling = false) }
            } else {
                failEnrolment(message)
            }
        }
    }

    fun onRemoveRequested(passkey: Passkey) {
        _state.update { it.copy(removing = passkey) }
    }

    fun onRemoveDismissed() {
        _state.update { it.copy(removing = null) }
    }

    fun onRemoveConfirmed() {
        val passkey = _state.value.removing ?: return

        viewModelScope.launch {
            _state.update { it.copy(removing = null) }
            passkeyService
                .delete(passkey.credentialId)
                .onSuccess {
                    _state.update { current ->
                        current.copy(
                            passkeys =
                                current.passkeys.filterNot {
                                    it.credentialId ==
                                        passkey.credentialId
                                },
                        )
                    }
                    eventChannel.send(PasskeysEvent.ShowMessage(UiText.Resource(Res.string.passkeys_removed)))
                }.onFailure { error ->
                    eventChannel.send(PasskeysEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            passkeyService
                .passkeys()
                .onSuccess { passkeys ->
                    _state.update { it.copy(passkeys = passkeys, isLoading = false) }
                }.onFailure { error ->
                    _state.update { it.copy(isLoading = false) }
                    eventChannel.send(PasskeysEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    private suspend fun failEnrolment(message: UiText) {
        pendingCeremonyId = null
        _state.update { it.copy(isEnrolling = false) }
        eventChannel.send(PasskeysEvent.ShowMessage(message))
    }
}
