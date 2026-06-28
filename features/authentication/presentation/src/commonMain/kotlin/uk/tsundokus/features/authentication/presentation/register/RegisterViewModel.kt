package uk.tsundokus.features.authentication.presentation.register

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.error_account_exists
import tsundokuapp.features.authentication.presentation.generated.resources.error_display_name_invalid
import tsundokuapp.features.authentication.presentation.generated.resources.register_email_invalid
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_mismatch
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_requirements
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.domain.validation.PasswordValidator
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.authentication.domain.AuthService
import uk.tsundokus.features.authentication.domain.DisplayNameValidator
import uk.tsundokus.features.authentication.domain.EmailValidator
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class RegisterViewModel(
    private val authService: AuthService,
) : ViewModel() {
    private val eventChannel = Channel<RegisterEvent>()
    val events = eventChannel.receiveAsFlow()

    private var hasLoadedInitialData = false

    private val _state = MutableStateFlow(RegisterState())
    val state =
        _state
            .onStart {
                if (!hasLoadedInitialData) {
                    observeFieldClearOnEdit()
                    hasLoadedInitialData = true
                }
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5.seconds),
                initialValue = _state.value,
            )

    private fun observeFieldClearOnEdit() {
        snapshotFlow {
            _state.value.displayNameTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.displayNameError != null) {
                    it.copy(displayNameError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.emailTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.emailError != null) {
                    it.copy(emailError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.passwordTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.passwordError != null) {
                    it.copy(passwordError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)

        snapshotFlow {
            _state.value.confirmPasswordTextState.text
                .toString()
        }.onEach {
            _state.update {
                if (it.confirmPasswordError != null) {
                    it.copy(confirmPasswordError = null)
                } else {
                    it
                }
            }
        }.launchIn(viewModelScope)
    }

    fun validateDisplayNameOnBlur() {
        val displayName =
            state.value.displayNameTextState.text
                .toString()
        if (displayName.isEmpty()) return
        val error =
            if (!DisplayNameValidator.validate(displayName)) {
                UiText.Resource(Res.string.error_display_name_invalid)
            } else {
                null
            }
        _state.update { it.copy(displayNameError = error) }
    }

    fun validateEmailOnBlur() {
        val email =
            state.value.emailTextState.text
                .toString()
        if (email.isEmpty()) return
        val error =
            if (!EmailValidator.validate(email)) {
                UiText.Resource(Res.string.register_email_invalid)
            } else {
                null
            }
        _state.update { it.copy(emailError = error) }
    }

    fun validatePasswordOnBlur() {
        val password =
            state.value.passwordTextState.text
                .toString()
        if (password.isEmpty()) return
        val error =
            if (!PasswordValidator.validate(password)) {
                UiText.Resource(Res.string.register_password_requirements)
            } else {
                null
            }
        _state.update { it.copy(passwordError = error) }
    }

    fun validateConfirmPasswordOnBlur() {
        val confirmPassword =
            state.value.confirmPasswordTextState.text
                .toString()
        if (confirmPassword.isEmpty()) return

        val password =
            state.value.passwordTextState.text
                .toString()
        val error =
            if (confirmPassword != password) {
                UiText.Resource(Res.string.register_password_mismatch)
            } else {
                null
            }
        _state.update { it.copy(confirmPasswordError = error) }
    }

    fun togglePasswordVisibility() {
        _state.update {
            it.copy(
                isPasswordVisible = !it.isPasswordVisible,
            )
        }
    }

    fun toggleConfirmPasswordVisibility() {
        _state.update {
            it.copy(
                isConfirmPasswordVisible = !it.isConfirmPasswordVisible,
            )
        }
    }

    fun register() {
        if (!validateFormInputs()) {
            return
        }

        viewModelScope.launch {
            _state.update {
                it.copy(
                    isRegistering = true,
                )
            }

            val name =
                state.value.displayNameTextState.text
                    .toString()
            val email =
                state.value.emailTextState.text
                    .toString()
            val password =
                state.value.passwordTextState.text
                    .toString()

            authService
                .register(
                    name = name,
                    email = email,
                    password = password,
                ).onSuccess {
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterEvent.RegistrationSuccess(email))
                }.onFailure { error ->
                    val registrationError =
                        when (error) {
                            DataError.Remote.CONFLICT -> UiText.Resource(Res.string.error_account_exists)
                            else -> error.toUiText()
                        }
                    _state.update { it.copy(isRegistering = false) }
                    eventChannel.send(RegisterEvent.RegistrationError(registrationError))
                }
        }
    }

    private fun validateFormInputs(): Boolean {
        val currentState = state.value
        val displayName = currentState.displayNameTextState.text.toString()
        val email = currentState.emailTextState.text.toString()
        val password = currentState.passwordTextState.text.toString()
        val confirmPassword = currentState.confirmPasswordTextState.text.toString()

        val isDisplayNameValid = DisplayNameValidator.validate(displayName)
        val isEmailValid = EmailValidator.validate(email)
        val isPasswordValid = PasswordValidator.validate(password)
        val isConfirmPasswordValid = confirmPassword == password

        _state.update {
            it.copy(
                displayNameError =
                    if (!isDisplayNameValid) UiText.Resource(Res.string.error_display_name_invalid) else null,
                emailError = if (!isEmailValid) UiText.Resource(Res.string.register_email_invalid) else null,
                passwordError =
                    if (!isPasswordValid) {
                        UiText.Resource(
                            Res.string.register_password_requirements,
                        )
                    } else {
                        null
                    },
                confirmPasswordError =
                    if (!isConfirmPasswordValid) {
                        UiText.Resource(Res.string.register_password_mismatch)
                    } else {
                        null
                    },
            )
        }

        return isDisplayNameValid && isEmailValid && isPasswordValid && isConfirmPasswordValid
    }
}
