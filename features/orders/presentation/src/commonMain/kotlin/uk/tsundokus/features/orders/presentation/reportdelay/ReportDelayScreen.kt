package uk.tsundokus.features.orders.presentation.reportdelay

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents

@Composable
fun ReportDelayRoot(
    orderId: String,
    onSaved: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ReportDelayViewModel =
        koinViewModel(
            key = orderId,
            parameters = { parametersOf(orderId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReportDelayEvent.Saved -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
                onSaved()
            }

            is ReportDelayEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    ReportDelayScreen(
        state = state,
        onDateChange = viewModel::onDateChange,
        onSave = viewModel::onSave,
    )
}

@Composable
private fun ReportDelayScreen(
    state: ReportDelayState,
    onDateChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Text(
            text = "New expected date",
            style = MaterialTheme.typography.titleMedium,
        )
        VerticalSpacer(4.dp)
        Text(
            text = "When do you now expect this order to arrive?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        VerticalSpacer(16.dp)
        OutlinedTextField(
            value = state.date,
            onValueChange = onDateChange,
            label = { Text("Expected date") },
            placeholder = { Text("YYYY-MM-DD") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(16.dp)
        TsundokuButton(
            text = "Save delay",
            onClick = onSave,
            enabled = state.canSave,
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@PreviewThemes
@Composable
private fun ReportDelayScreenPreview() {
    TsundokuTheme {
        Surface {
            ReportDelayScreen(
                state = ReportDelayState(date = "2026-08-15"),
                onDateChange = {},
                onSave = {},
            )
        }
    }
}
