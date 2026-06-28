package uk.tsundokus.features.authentication.presentation.resetpassword

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.InjectedParam
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.error_reset_password_token_invalid
import tsundokuapp.features.authentication.presentation.generated.resources.error_same_password
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.domain.validation.PasswordValidator
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.authentication.domain.AuthService

@KoinViewModel
class ResetPasswordViewModel(
    private val authService: AuthService,
    @InjectedParam private val token: String,
) : ViewModel() {
    private var hasLoadedInitialData = false

    private val isPasswordValidFlow =
        snapshotFlow {
            state.value.passwordTextState.text
                .toString()
        }.map { password -> PasswordValidator.validate(password) }
            .distinctUntilChanged()

    private val _state = MutableStateFlow(ResetPasswordState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeValidationState()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = _state.value,
            )

    fun onTogglePasswordVisibilityClick() {
        _state.value =
            _state.value.copy(
                isPasswordVisible = !_state.value.isPasswordVisible,
            )
    }

    fun submitResetPasswordRequest() {
        if (state.value.isLoading || !state.value.canSubmit) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isLoading = true,
                    isResetSuccessful = false,
                )
            }

            val newPassword =
                state.value.passwordTextState.text
                    .toString()
            authService
                .resetPassword(
                    newPassword = newPassword,
                    token = token,
                ).onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isResetSuccessful = true,
                            errorText = null,
                        )
                    }
                }.onFailure { error ->
                    val errorText =
                        when (error) {
                            DataError.Remote.UNAUTHORIZED -> {
                                UiText.Resource(
                                    Res.string.error_reset_password_token_invalid,
                                )
                            }

                            DataError.Remote.CONFLICT -> {
                                UiText.Resource(Res.string.error_same_password)
                            }

                            else -> {
                                error.toUiText()
                            }
                        }
                    _state.update {
                        it.copy(
                            errorText = errorText,
                            isLoading = false,
                        )
                    }
                }
        }
    }

    private fun observeValidationState() {
        isPasswordValidFlow
            .onEach { isPasswordValid ->
                _state.update {
                    it.copy(
                        canSubmit = isPasswordValid,
                    )
                }
            }.launchIn(viewModelScope)
    }
}
