package uk.tsundokus.features.authentication.presentation.emailverification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_back_to_sign_in
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_continue
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_failed_desc
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_failed_title
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_success_desc
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_success_title
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_verifying
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.designsystem.theme.extended

@Composable
fun EmailVerificationScreenRoot(
    token: String,
    onContinue: () -> Unit,
    emailVerificationViewModel: EmailVerificationViewModel = koinViewModel(parameters = { parametersOf(token) }),
) {
    val state by emailVerificationViewModel.state.collectAsStateWithLifecycle()

    EmailVerificationScreen(
        state = state,
        onContinueClick = onContinue,
    )
}

@Composable
private fun EmailVerificationScreen(
    state: EmailVerificationState,
    onContinueClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when {
            state.isVerifying -> VerifyingContent()
            state.isVerified -> VerifiedContent(onContinueClick = onContinueClick)
            else -> FailedContent(onContinueClick = onContinueClick)
        }
    }
}

@Composable
private fun VerifyingContent() {
    CircularProgressIndicator(
        modifier = Modifier.size(48.dp),
        color = MaterialTheme.colorScheme.primary,
    )
    Text(
        text = stringResource(Res.string.email_verification_verifying),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun VerifiedContent(onContinueClick: () -> Unit) {
    Icon(
        imageVector = TsundokuIcons.CheckCircle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.extended.positive,
        modifier = Modifier.size(56.dp),
    )
    Text(
        text = stringResource(Res.string.email_verification_success_title),
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(Res.string.email_verification_success_desc),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    VerticalSpacer(8.dp)
    TsundokuButton(
        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        text = stringResource(Res.string.email_verification_continue),
        onClick = onContinueClick,
    )
}

@Composable
private fun FailedContent(onContinueClick: () -> Unit) {
    Icon(
        imageVector = TsundokuIcons.Warning,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
        modifier = Modifier.size(56.dp),
    )
    Text(
        text = stringResource(Res.string.email_verification_failed_title),
        style = MaterialTheme.typography.headlineSmall,
        color = MaterialTheme.colorScheme.error,
        textAlign = TextAlign.Center,
    )
    Text(
        text = stringResource(Res.string.email_verification_failed_desc),
        style = MaterialTheme.typography.bodyMedium,
        textAlign = TextAlign.Center,
    )
    VerticalSpacer(8.dp)
    TsundokuButton(
        modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
        text = stringResource(Res.string.email_verification_back_to_sign_in),
        style = TsundokuButtonStyle.Secondary,
        onClick = onContinueClick,
    )
}

@PreviewThemes
@Composable
private fun EmailVerificationVerifyingPreview() {
    TsundokuTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerifying = true),
                onContinueClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationVerifiedPreview() {
    TsundokuTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerifying = false, isVerified = true),
                onContinueClick = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun EmailVerificationFailedPreview() {
    TsundokuTheme {
        Surface {
            EmailVerificationScreen(
                state = EmailVerificationState(isVerifying = false, isVerified = false),
                onContinueClick = {},
            )
        }
    }
}
