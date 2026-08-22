package uk.tsundokus.features.authentication.presentation.register

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.KoinApplicationPreview
import org.koin.compose.viewmodel.koinViewModel
import org.koin.plugin.module.dsl.modules
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.register_already_have_account_prefix
import tsundokuapp.features.authentication.presentation.generated.resources.register_button
import tsundokuapp.features.authentication.presentation.generated.resources.register_confirm_password_hint
import tsundokuapp.features.authentication.presentation.generated.resources.register_display_name_hint
import tsundokuapp.features.authentication.presentation.generated.resources.register_email_hint
import tsundokuapp.features.authentication.presentation.generated.resources.register_login_action
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_hint
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_requirements
import tsundokuapp.features.authentication.presentation.generated.resources.register_privacy_notice_prefix
import tsundokuapp.features.authentication.presentation.generated.resources.register_privacy_notice_suffix
import tsundokuapp.features.authentication.presentation.generated.resources.register_privacy_policy_link
import tsundokuapp.features.authentication.presentation.generated.resources.register_title
import tsundokuapp.features.authentication.presentation.generated.resources.register_title_desc
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.text.TsundokuInlineLinkText
import uk.tsundokus.core.designsystem.textfields.TsundokuPasswordTextField
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.designsystem.theme.headlineLargeBold
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.authentication.presentation.di.AuthPresentationModule
import uk.tsundokus.features.authentication.presentation.navigation.SignIn
import uk.tsundokus.features.authentication.presentation.navigation.SignUp

@Composable
fun RegisterRoot(
    backStack: NavBackStack<NavKey>,
    snackbarHostState: SnackbarHostState,
    onRegisterSuccess: (email: String) -> Unit,
    modifier: Modifier = Modifier,
    registerViewModel: RegisterViewModel = koinViewModel(),
) {
    val state by registerViewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(registerViewModel.events) { event ->
        when (event) {
            is RegisterEvent.RegistrationSuccess -> {
                onRegisterSuccess(event.email)
            }

            is RegisterEvent.RegistrationError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    RegisterScreen(
        state = state,
        onLoginClick = {
            backStack.add(SignIn)
            backStack.remove(SignUp)
        },
        onCreateAccountClick = { registerViewModel.register() },
        togglePasswordVisibility = registerViewModel::togglePasswordVisibility,
        toggleConfirmPasswordVisibility = registerViewModel::toggleConfirmPasswordVisibility,
        onDisplayNameFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validateDisplayNameOnBlur() },
        onEmailFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validateEmailOnBlur() },
        onPasswordFocusChanged = { hasFocus -> if (!hasFocus) registerViewModel.validatePasswordOnBlur() },
        onConfirmPasswordFocusChanged = { hasFocus ->
            if (!hasFocus) registerViewModel.validateConfirmPasswordOnBlur()
        },
        modifier = modifier,
    )
}

@Composable
private fun RegisterScreen(
    state: RegisterState,
    onLoginClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    togglePasswordVisibility: () -> Unit,
    toggleConfirmPasswordVisibility: () -> Unit,
    onDisplayNameFocusChanged: (Boolean) -> Unit,
    onEmailFocusChanged: (Boolean) -> Unit,
    onPasswordFocusChanged: (Boolean) -> Unit,
    onConfirmPasswordFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        Text(
            text = stringResource(Res.string.register_title),
            style = MaterialTheme.typography.headlineLargeBold,
        )
        VerticalSpacer(8.dp)
        Text(
            text = stringResource(Res.string.register_title_desc),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 300.dp),
        )
        VerticalSpacer(36.dp)
        TsundokuTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.displayNameTextState,
            placeholder = stringResource(Res.string.register_display_name_hint),
            supportingText = state.displayNameError?.asString().orEmpty(),
            isError = state.displayNameError != null,
            singleLine = true,
            onFocusChanged = onDisplayNameFocusChanged,
            contentType = ContentType.Username,
        )
        state.displayNameError?.let {
            VerticalSpacer(16.dp)
        }
        TsundokuTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.emailTextState,
            placeholder = stringResource(Res.string.register_email_hint),
            supportingText = state.emailError?.asString().orEmpty(),
            isError = state.emailError != null,
            singleLine = true,
            keyboardType = KeyboardType.Email,
            onFocusChanged = onEmailFocusChanged,
            contentType = ContentType.EmailAddress,
        )
        state.emailError?.let {
            VerticalSpacer(16.dp)
        }
        TsundokuPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.passwordTextState,
            placeholder = stringResource(Res.string.register_password_hint),
            supportingText =
                state.passwordError?.asString() ?: stringResource(Res.string.register_password_requirements),
            isError = state.passwordError != null,
            onToggleVisibilityClick = togglePasswordVisibility,
            isPasswordVisible = state.isPasswordVisible,
            onFocusChanged = onPasswordFocusChanged,
        )
        state.passwordError?.let {
            VerticalSpacer(16.dp)
        }
        TsundokuPasswordTextField(
            modifier = Modifier.widthIn(max = 300.dp),
            state = state.confirmPasswordTextState,
            placeholder = stringResource(Res.string.register_confirm_password_hint),
            supportingText = state.confirmPasswordError?.asString().orEmpty(),
            isError = state.confirmPasswordError != null,
            onToggleVisibilityClick = toggleConfirmPasswordVisibility,
            isPasswordVisible = state.isConfirmPasswordVisible,
            onFocusChanged = onConfirmPasswordFocusChanged,
        )
        VerticalSpacer(16.dp)
        TsundokuButton(
            modifier =
                Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth(),
            onClick = onCreateAccountClick,
            text = stringResource(Res.string.register_button),
            enabled = !state.isRegistering,
            isLoading = state.isRegistering,
        )
        VerticalSpacer(16.dp)
        PrivacyNotice(modifier = Modifier.widthIn(max = 300.dp))
        VerticalSpacer(16.dp)
        TsundokuInlineLinkText(
            textBeforeLink = stringResource(Res.string.register_already_have_account_prefix),
            linkText = stringResource(Res.string.register_login_action),
            onLinkClick = onLoginClick,
        )
    }
}

private const val PRIVACY_POLICY_URL = "https://github.com/diasDominik/tsundokus-app/blob/main/PRIVACY.md"

/**
 * Informational fine print shown where the account is created, so the privacy policy is one tap away
 * at the moment data is collected. No consent checkbox: sign-up data is processed to perform the
 * contract, not on the basis of consent.
 */
@Composable
private fun PrivacyNotice(modifier: Modifier = Modifier) {
    val uriHandler = LocalUriHandler.current
    TsundokuInlineLinkText(
        modifier = modifier,
        textBeforeLink = stringResource(Res.string.register_privacy_notice_prefix),
        linkText = stringResource(Res.string.register_privacy_policy_link),
        textAfterLink = stringResource(Res.string.register_privacy_notice_suffix),
        onLinkClick = { uriHandler.openUri(PRIVACY_POLICY_URL) },
        textStyle = MaterialTheme.typography.bodySmall.copy(textAlign = TextAlign.Center),
        textColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@PreviewThemes
@Composable
private fun RegisterScreenPreview() {
    KoinApplicationPreview(
        application = {
            modules(AuthPresentationModule::class)
        },
    ) {
        TsundokuTheme {
            Surface {
                RegisterScreen(
                    state =
                        RegisterState(
                            displayNameTextState = TextFieldState(""),
                            displayNameError = null,
                        ),
                    onLoginClick = {},
                    togglePasswordVisibility = {},
                    toggleConfirmPasswordVisibility = {},
                    onCreateAccountClick = {},
                    onDisplayNameFocusChanged = {},
                    onEmailFocusChanged = {},
                    onPasswordFocusChanged = {},
                    onConfirmPasswordFocusChanged = {},
                )
            }
        }
    }
}
