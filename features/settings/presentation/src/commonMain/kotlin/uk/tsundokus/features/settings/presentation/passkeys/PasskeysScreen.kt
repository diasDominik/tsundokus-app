package uk.tsundokus.features.settings.presentation.passkeys

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.androidpoet.passkeys.PasskeyException
import io.github.androidpoet.passkeys.PasskeyResult
import io.github.androidpoet.passkeys.compose.rememberPasskeyClient
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.error_passkey_enrolment_failed
import tsundokuapp.features.settings.presentation.generated.resources.error_passkey_unsupported
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_add
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_added_on
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_empty
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_explainer
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_last_used
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_never_used
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_remove
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_remove_cancel
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_remove_confirm
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_remove_message
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_remove_title
import tsundokuapp.features.settings.presentation.generated.resources.passkeys_unnamed
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.dialog.TsundokuConfirmDialog
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.isPasskeySupported
import uk.tsundokus.features.settings.domain.passkeys.Passkey

@Composable
fun PasskeysRoot(
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    viewModel: PasskeysViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Nothing from the passkey libraries is touched where the platform cannot run a ceremony: they
    // require Android 9, and the app supports Android 8.
    val passkeysSupported = remember { isPasskeySupported() }
    val passkeyClient = if (passkeysSupported) rememberPasskeyClient() else null

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is PasskeysEvent.ShowMessage -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }

            // The options go to the authenticator exactly as the server minted them, and what it
            // signs goes back untouched: everything in between is the platform's business.
            is PasskeysEvent.RunEnrolmentCeremony -> {
                when (val result = passkeyClient?.create(event.optionsJson)) {
                    null -> Unit
                    is PasskeyResult.Success -> viewModel.onEnrolmentResponse(result.value.rawJson)
                    is PasskeyResult.Failure -> viewModel.onEnrolmentCeremonyFailed(result.error.toUiText())
                }
            }
        }
    }

    PasskeysScreen(
        state = state,
        canAddPasskey = passkeysSupported,
        onAddPasskey = viewModel::onAddPasskey,
        onRemoveRequested = viewModel::onRemoveRequested,
        onRemoveDismissed = viewModel::onRemoveDismissed,
        onRemoveConfirmed = viewModel::onRemoveConfirmed,
        modifier = modifier,
    )
}

/**
 * Maps a failed ceremony to something worth showing. A cancellation is the user's own decision and
 * says nothing they do not already know, so it shows nothing at all.
 */
private fun PasskeyException.toUiText(): UiText? =
    when (this) {
        is PasskeyException.UserCanceled -> null
        is PasskeyException.Unsupported -> UiText.Resource(Res.string.error_passkey_unsupported)
        else -> UiText.Resource(Res.string.error_passkey_enrolment_failed)
    }

@Composable
private fun PasskeysScreen(
    state: PasskeysState,
    canAddPasskey: Boolean,
    onAddPasskey: () -> Unit,
    onRemoveRequested: (Passkey) -> Unit,
    onRemoveDismissed: () -> Unit,
    onRemoveConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(Res.string.passkeys_explainer),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer(16.dp)

            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                state.passkeys.isEmpty() -> {
                    Text(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        text = stringResource(Res.string.passkeys_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }

                else -> {
                    state.passkeys.forEach { passkey ->
                        PasskeyRow(passkey = passkey, onRemove = { onRemoveRequested(passkey) })
                        HorizontalDivider()
                    }
                }
            }

            VerticalSpacer(16.dp)
            if (canAddPasskey) {
                TsundokuButton(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(Res.string.passkeys_add),
                    onClick = onAddPasskey,
                    isLoading = state.isEnrolling,
                )
            } else {
                // The passkeys already on the account stay listed and removable here; this device
                // simply cannot make another one.
                Text(
                    text = stringResource(Res.string.error_passkey_unsupported),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    state.removing?.let {
        TsundokuConfirmDialog(
            title = stringResource(Res.string.passkeys_remove_title),
            message = stringResource(Res.string.passkeys_remove_message),
            confirmText = stringResource(Res.string.passkeys_remove_confirm),
            dismissText = stringResource(Res.string.passkeys_remove_cancel),
            onConfirm = onRemoveConfirmed,
            onDismiss = onRemoveDismissed,
            isDestructive = true,
        )
    }
}

@Composable
private fun PasskeyRow(
    passkey: Passkey,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(imageVector = TsundokuIcons.Lock, contentDescription = null)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = passkey.label ?: stringResource(Res.string.passkeys_unnamed),
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = stringResource(Res.string.passkeys_added_on, passkey.createdAt.toDay()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    passkey.lastUsedAt
                        ?.let { stringResource(Res.string.passkeys_last_used, it.toDay()) }
                        ?: stringResource(Res.string.passkeys_never_used),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            modifier = Modifier.clickable(onClick = onRemove).padding(8.dp),
            text = stringResource(Res.string.passkeys_remove),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** The day out of an ISO-8601 instant. Which day a passkey was added is all this list needs. */
private fun String.toDay(): String = substringBefore('T')

@PreviewThemes
@Composable
private fun PasskeysScreenPreview() {
    TsundokuTheme {
        Surface {
            PasskeysScreen(
                state =
                    PasskeysState(
                        isLoading = false,
                        passkeys =
                            listOf(
                                Passkey(
                                    credentialId = "a",
                                    label = "Pixel 9 Pro",
                                    createdAt = "2026-09-20T10:15:00Z",
                                    lastUsedAt = "2026-09-22T08:02:00Z",
                                ),
                                Passkey(
                                    credentialId = "b",
                                    label = null,
                                    createdAt = "2026-09-21T19:40:00Z",
                                    lastUsedAt = null,
                                ),
                            ),
                    ),
                canAddPasskey = true,
                onAddPasskey = { },
                onRemoveRequested = { },
                onRemoveDismissed = { },
                onRemoveConfirmed = { },
            )
        }
    }
}
