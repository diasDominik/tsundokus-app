package uk.tsundokus.features.settings.presentation.deleteaccount

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.settings.domain.account.AccountService
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class DeleteAccountViewModel(
    private val accountService: AccountService,
    private val sessionStorage: SessionStorage,
) : ViewModel() {
    val confirmationState = TextFieldState()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(DeleteAccountState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeConfirmation()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private val eventChannel = Channel<DeleteAccountEvent>()
    val events = eventChannel.receiveAsFlow()

    fun onDelete() {
        if (_state.value.isSubmitting || !_state.value.confirmationValid) return
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            accountService
                .deleteAccount()
                .onSuccess {
                    // Clear the session so the app returns to the Welcome flow.
                    sessionStorage.set(null)
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(DeleteAccountEvent.Deleted)
                }.onFailure { error ->
                    _state.update { it.copy(isSubmitting = false) }
                    eventChannel.send(DeleteAccountEvent.Error(error.toUiText()))
                }
        }
    }

    private fun observeConfirmation() {
        snapshotFlow { confirmationState.text.toString() }
            .map { it == CONFIRM_KEYWORD }
            .distinctUntilChanged()
            .onEach { valid -> _state.update { it.copy(confirmationValid = valid) } }
            .launchIn(viewModelScope)
    }

    private companion object {
        private const val CONFIRM_KEYWORD = "DELETE"
    }
}
