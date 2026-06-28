package uk.tsundokus.features.authentication.presentation.forgotpassword

import androidx.compose.foundation.text.input.TextFieldState
import uk.tsundokus.core.presentation.util.UiText

data class ForgotPasswordState(
    val emailTextFieldState: TextFieldState = TextFieldState(),
    val canSubmit: Boolean = false,
    val isLoading: Boolean = false,
    val errorText: UiText? = null,
    val isEmailSentSuccessfully: Boolean = false,
)
