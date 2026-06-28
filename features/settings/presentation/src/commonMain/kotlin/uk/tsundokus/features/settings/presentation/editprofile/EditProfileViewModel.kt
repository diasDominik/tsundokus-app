package uk.tsundokus.features.settings.presentation.editprofile

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
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.settings.domain.account.AccountService

@KoinViewModel
class EditProfileViewModel(
    sessionStorage: SessionStorage,
    private val accountService: AccountService,
) : ViewModel() {
    val nameState =
        TextFieldState(
            sessionStorage
                .get()
                ?.user
                ?.username
                .orEmpty(),
        )

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    private val eventChannel = Channel<EditProfileEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onSave() {
        if (_state.value.isSubmitting) return
        val name = nameState.text.toString().trim()
        if (name.isBlank()) {
            send(EditProfileEvent.Error(UiText.DynamicString("Display name is required")))
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            accountService
                .updateProfile(name)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(EditProfileEvent.Saved)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(EditProfileEvent.Error(error.toUiText()))
                }
        }
    }

    private fun send(event: EditProfileEvent) {
        viewModelScope.launch { eventChannel.send(event) }
    }
}
