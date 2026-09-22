package uk.tsundokus.features.authentication.presentation.login

import androidx.compose.foundation.text.input.TextFieldState

data class LoginState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val passwordTextFieldState: TextFieldState = TextFieldState(),
    val isPasswordVisible: Boolean = false,
    val canLogin: Boolean = false,
    val isLoggingIn: Boolean = false,
    /** Set from the moment the passkey button is pressed until the ceremony resolves either way. */
    val isSigningInWithPasskey: Boolean = false,
)
