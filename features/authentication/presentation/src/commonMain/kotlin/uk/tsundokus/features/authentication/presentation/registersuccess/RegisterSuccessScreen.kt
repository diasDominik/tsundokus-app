package uk.tsundokus.features.authentication.presentation.registersuccess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.email_verification_back_to_sign_in
import tsundokuapp.features.authentication.presentation.generated.resources.register_success_desc
import tsundokuapp.features.authentication.presentation.generated.resources.register_success_title
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme

@Composable
fun RegisterSuccessScreenRoot(
    email: String,
    onBackToSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    RegisterSuccessScreen(
        email = email,
        onBackToSignInClick = onBackToSignIn,
        modifier = modifier,
    )
}

@Composable
private fun RegisterSuccessScreen(
    email: String,
    onBackToSignInClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = TsundokuIcons.Email,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(56.dp),
        )
        Text(
            text = stringResource(Res.string.register_success_title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.register_success_desc, email),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        TsundokuButton(
            modifier = Modifier.widthIn(max = 300.dp).fillMaxWidth(),
            text = stringResource(Res.string.email_verification_back_to_sign_in),
            onClick = onBackToSignInClick,
        )
    }
}

@PreviewThemes
@Composable
private fun RegisterSuccessPreview() {
    TsundokuTheme {
        Surface {
            RegisterSuccessScreen(
                email = "reader@example.com",
                onBackToSignInClick = {},
            )
        }
    }
}
