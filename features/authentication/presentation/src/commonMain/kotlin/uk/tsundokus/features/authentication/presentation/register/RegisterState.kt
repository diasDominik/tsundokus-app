package uk.tsundokus.features.authentication.presentation.register

import androidx.compose.foundation.text.input.TextFieldState
import uk.tsundokus.core.presentation.util.UiText

data class RegisterState(
    val displayNameTextState: TextFieldState = TextFieldState(),
    val displayNameError: UiText? = null,
    val emailTextState: TextFieldState = TextFieldState(),
    val emailError: UiText? = null,
    val passwordTextState: TextFieldState = TextFieldState(),
    val passwordError: UiText? = null,
    val confirmPasswordTextState: TextFieldState = TextFieldState(),
    val confirmPasswordError: UiText? = null,
    val isRegistering: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
)
