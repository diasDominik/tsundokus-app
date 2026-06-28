package uk.tsundokus.features.settings.presentation.changeemail

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.settings.domain.account.AccountService

@KoinViewModel
class ChangeEmailViewModel(
    private val accountService: AccountService,
) : ViewModel() {
    val newEmailState = TextFieldState()
    val currentPasswordState = TextFieldState()

    private val _state = MutableStateFlow(ChangeEmailState())
    val state: StateFlow<ChangeEmailState> = _state.asStateFlow()

    private val eventChannel = Channel<ChangeEmailEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSave() {
        if (_state.value.isSubmitting) return
        val email = newEmailState.text.toString().trim()
        val password = currentPasswordState.text.toString()
        if (!email.isValidEmail()) {
            send(ChangeEmailEvent.Error(UiText.DynamicString("Enter a valid email address")))
            return
        }
        if (password.isBlank()) {
            send(ChangeEmailEvent.Error(UiText.DynamicString("Current password is required")))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            accountService
                .changeEmail(newEmail = email, currentPassword = password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(ChangeEmailEvent.Saved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(ChangeEmailEvent.Error(error.toUiText()))
                }
        }
    }

    private fun send(event: ChangeEmailEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private fun String.isValidEmail(): Boolean = contains("@") && substringAfter("@").contains(".")
}
