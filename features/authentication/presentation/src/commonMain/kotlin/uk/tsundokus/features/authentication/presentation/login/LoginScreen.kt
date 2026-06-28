package uk.tsundokus.features.authentication.presentation.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.login
import tsundokuapp.features.authentication.presentation.generated.resources.login_create_account
import tsundokuapp.features.authentication.presentation.generated.resources.login_forgot_password
import tsundokuapp.features.authentication.presentation.generated.resources.login_sign_up
import tsundokuapp.features.authentication.presentation.generated.resources.login_title
import tsundokuapp.features.authentication.presentation.generated.resources.login_title_desc
import tsundokuapp.features.authentication.presentation.generated.resources.register_email_hint
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_hint
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.text.TsundokuInlineLinkText
import uk.tsundokus.core.designsystem.textfields.TsundokuPasswordTextField
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.authentication.presentation.navigation.ForgotPassword
import uk.tsundokus.features.authentication.presentation.navigation.SignUp

@Composable
fun LoginRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    onLoginSuccess: () -> Unit,
    loginViewModel: LoginViewModel = koinViewModel(),
) {
    val state by loginViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(loginViewModel.events) {
        when (it) {
            is LoginEvent.LoginFailure -> {
                snackbarHostState.showSnackbar(
                    message = it.error.asStringAsync(),
                )
            }

            LoginEvent.LoginSuccess -> {
                onLoginSuccess()
            }
        }
    }

    LoginScreen(
        emailTextFieldState = state.emailTextFieldState,
        passwordTextFieldState = state.passwordTextFieldState,
        isPasswordVisible = state.isPasswordVisible,
        canLogin = state.canLogin,
        isLoggingIn = state.isLoggingIn,
        onTogglePasswordVisibility = loginViewModel::onTogglePasswordVisibility,
        onForgotPasswordClick = {
            backStack.add(ForgotPassword)
        },
        onLoginClick = loginViewModel::onLogin,
        onCreateAccountClick = {
            backStack.add(SignUp)
        },
    )
}

@Composable
private fun LoginScreen(
    emailTextFieldState: TextFieldState,
    passwordTextFieldState: TextFieldState,
    isPasswordVisible: Boolean,
    canLogin: Boolean,
    isLoggingIn: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onForgotPasswordClick: () -> Unit,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .padding(16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.login_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(Res.string.login_title_desc),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
        )
        TsundokuTextField(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            state = emailTextFieldState,
            placeholder = stringResource(Res.string.register_email_hint),
            keyboardType = KeyboardType.Email,
            contentType = ContentType.EmailAddress,
        )
        TsundokuPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            state = passwordTextFieldState,
            placeholder = stringResource(Res.string.register_password_hint),
            isPasswordVisible = isPasswordVisible,
            onToggleVisibilityClick = onTogglePasswordVisibility,
        )
        TsundokuInlineLinkText(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            textBeforeLink = "",
            linkText = stringResource(Res.string.login_forgot_password),
            textStyle = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.End),
            onLinkClick = onForgotPasswordClick,
        )
        VerticalSpacer(4.dp)
        TsundokuButton(
            modifier = Modifier.widthIn(max = 200.dp).fillMaxWidth(),
            text = stringResource(Res.string.login),
            onClick = onLoginClick,
            enabled = canLogin,
            isLoading = isLoggingIn,
        )
        HorizontalDivider()
        VerticalSpacer(4.dp)
        TsundokuInlineLinkText(
            modifier = Modifier.align(Alignment.Start),
            textBeforeLink = stringResource(Res.string.login_create_account),
            linkText = stringResource(Res.string.login_sign_up),
            onLinkClick = onCreateAccountClick,
        )
    }
}

@PreviewThemes
@Composable
private fun LoginScreenPreview() {
    TsundokuTheme {
        Surface {
            LoginScreen(
                emailTextFieldState = TextFieldState(""),
                passwordTextFieldState = TextFieldState(""),
                isPasswordVisible = false,
                canLogin = false,
                isLoggingIn = false,
                onTogglePasswordVisibility = { },
                onForgotPasswordClick = { },
                onLoginClick = { },
                onCreateAccountClick = { },
            )
        }
    }
}
