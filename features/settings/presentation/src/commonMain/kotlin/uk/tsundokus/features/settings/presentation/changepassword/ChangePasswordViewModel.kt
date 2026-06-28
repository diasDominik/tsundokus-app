package uk.tsundokus.features.settings.presentation.changepassword

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
class ChangePasswordViewModel(
    private val accountService: AccountService,
) : ViewModel() {
    val currentPasswordState = TextFieldState()
    val newPasswordState = TextFieldState()
    val confirmPasswordState = TextFieldState()

    private val _state = MutableStateFlow(ChangePasswordState())
    val state: StateFlow<ChangePasswordState> = _state.asStateFlow()

    private val eventChannel = Channel<ChangePasswordEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSave() {
        if (_state.value.isSubmitting) return
        val current = currentPasswordState.text.toString()
        val new = newPasswordState.text.toString()
        val confirm = confirmPasswordState.text.toString()
        if (current.isBlank() || new.isBlank()) {
            send(ChangePasswordEvent.Error(UiText.DynamicString("Please fill in all fields")))
            return
        }
        if (new.length < MIN_PASSWORD_LENGTH) {
            send(ChangePasswordEvent.Error(UiText.DynamicString("Password must be at least 6 characters")))
            return
        }
        if (new != confirm) {
            send(ChangePasswordEvent.Error(UiText.DynamicString("Passwords do not match")))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            accountService
                .changePassword(currentPassword = current, newPassword = new)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(ChangePasswordEvent.Saved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(ChangePasswordEvent.Error(error.toUiText()))
                }
        }
    }

    private fun send(event: ChangePasswordEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }

    private companion object {
        private const val MIN_PASSWORD_LENGTH = 6
    }
}
