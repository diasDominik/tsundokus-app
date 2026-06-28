package uk.tsundokus.features.authentication.presentation.forgotpassword

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.forgot_password
import tsundokuapp.features.authentication.presentation.generated.resources.forgot_password_desc
import tsundokuapp.features.authentication.presentation.generated.resources.forgot_password_email_sent_successfully
import tsundokuapp.features.authentication.presentation.generated.resources.forgot_password_submit
import tsundokuapp.features.authentication.presentation.generated.resources.register_email_hint
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.designsystem.theme.extended
import uk.tsundokus.core.presentation.util.UiText

@Composable
fun ForgotPasswordScreenRoot(
    modifier: Modifier = Modifier,
    forgotPasswordViewModel: ForgotPasswordViewModel = koinViewModel(),
) {
    val state by forgotPasswordViewModel.state.collectAsStateWithLifecycle()
    ForgotPasswordScreen(
        emailTextFieldState = state.emailTextFieldState,
        errorText = state.errorText,
        onSubmitClick = forgotPasswordViewModel::submitForgotPasswordRequest,
        modifier = modifier,
        enabled = !state.isLoading && state.canSubmit,
        isLoading = state.isLoading,
        isEmailSentSuccessfully = state.isEmailSentSuccessfully,
    )
}

@Composable
private fun ForgotPasswordScreen(
    emailTextFieldState: TextFieldState,
    errorText: UiText?,
    enabled: Boolean,
    isLoading: Boolean,
    isEmailSentSuccessfully: Boolean,
    onSubmitClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 32.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stringResource(Res.string.forgot_password),
            style = MaterialTheme.typography.headlineMedium,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.forgot_password_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        VerticalSpacer(16.dp)
        TsundokuTextField(
            state = emailTextFieldState,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            placeholder = stringResource(Res.string.register_email_hint),
            keyboardType = KeyboardType.Email,
            singleLine = true,
        )
        VerticalSpacer(16.dp)
        TsundokuButton(
            text = stringResource(Res.string.forgot_password_submit),
            onClick = onSubmitClick,
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            enabled = enabled,
            isLoading = isLoading,
        )
        if (isEmailSentSuccessfully) {
            VerticalSpacer(16.dp)
            Text(
                stringResource(Res.string.forgot_password_email_sent_successfully),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.extended.positive,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        errorText?.let {
            VerticalSpacer(16.dp)
            Text(
                text = errorText.asString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
@PreviewThemes
private fun ForgotPasswordScreenPreview() {
    TsundokuTheme {
        Surface {
            ForgotPasswordScreen(
                emailTextFieldState = TextFieldState(),
                errorText = UiText.DynamicString("This is an error message"),
                onSubmitClick = { },
                enabled = true,
                isLoading = false,
                isEmailSentSuccessfully = true,
            )
        }
    }
}
