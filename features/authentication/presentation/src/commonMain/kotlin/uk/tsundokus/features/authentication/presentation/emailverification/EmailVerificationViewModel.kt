package uk.tsundokus.features.authentication.presentation.emailverification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.authentication.domain.AuthService

@KoinViewModel
class EmailVerificationViewModel(
    private val authService: AuthService,
    @InjectedParam private val token: String,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(EmailVerificationState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    verifyEmail()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = _state.value,
            )

    private fun verifyEmail() {
        viewModelScope.launch {
            _state.update { it.copy(isVerifying = true) }

            authService
                .verifyEmail(token)
                .onSuccess {
                    _state.update { it.copy(isVerifying = false, isVerified = true) }
                }.onFailure {
                    _state.update { it.copy(isVerifying = false, isVerified = false) }
                }
        }
    }
}
