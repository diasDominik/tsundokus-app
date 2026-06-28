package uk.tsundokus.features.settings.presentation.deleteaccount

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.textfields.TsundokuTextField
import uk.tsundokus.core.presentation.util.ObserveAsEvents

@Composable
fun DeleteAccountRoot(
    snackbarHostState: SnackbarHostState,
    onDeleted: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DeleteAccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            DeleteAccountEvent.Deleted -> onDeleted()
            is DeleteAccountEvent.Error -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(modifier = Modifier.widthIn(max = 600.dp).fillMaxWidth()) {
            VerticalSpacer(8.dp)
            Text(
                text =
                    "This permanently deletes your account and all of your tracked orders. " +
                        "This action cannot be undone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer(16.dp)
            TsundokuTextField(
                state = viewModel.confirmationState,
                title = "Type DELETE to confirm",
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(24.dp)
            TsundokuButton(
                text = "Delete account",
                onClick = viewModel::onDelete,
                style = TsundokuButtonStyle.DestructivePrimary,
                enabled = state.confirmationValid && !state.isSubmitting,
                isLoading = state.isSubmitting,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
