package uk.tsundokus.features.orders.presentation.export

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.export_empty
import tsundokuapp.features.orders.presentation.generated.resources.export_format_csv
import tsundokuapp.features.orders.presentation.generated.resources.export_format_csv_caption
import tsundokuapp.features.orders.presentation.generated.resources.export_format_json
import tsundokuapp.features.orders.presentation.generated.resources.export_format_json_caption
import tsundokuapp.features.orders.presentation.generated.resources.export_intro
import tsundokuapp.features.orders.presentation.generated.resources.export_save
import tsundokuapp.features.orders.presentation.generated.resources.export_summary
import tsundokuapp.features.orders.presentation.generated.resources.export_summary_with_cancelled
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.domain.export.ExportFormat

@Composable
fun ExportRoot(
    snackbar: SnackbarController,
    viewModel: ExportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val saver = rememberExportSaver()
    val scope = rememberCoroutineScope()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ExportEvent.SaveFile -> {
                scope.launch { viewModel.onAction(ExportAction.OnSaveFinished(saver.save(event.file))) }
            }

            is ExportEvent.ShowMessage -> {
                snackbar.show(event.message)
            }
        }
    }

    ExportScreen(state = state, onAction = viewModel::onAction)
}

@Composable
private fun ExportScreen(
    state: ExportState,
    onAction: (ExportAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier =
                Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = stringResource(Res.string.export_intro), style = MaterialTheme.typography.bodyLarge)
            if (state.orderCount == 0) {
                Text(
                    text = stringResource(Res.string.export_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                return@Column
            }
            FormatPicker(selected = state.format, onSelect = { onAction(ExportAction.OnFormatSelected(it)) })
            Text(
                text = stringResource(state.format.captionRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text =
                    if (state.cancelledCount > 0) {
                        pluralStringResource(
                            Res.plurals.export_summary_with_cancelled,
                            state.orderCount,
                            state.orderCount,
                            state.cancelledCount,
                        )
                    } else {
                        pluralStringResource(Res.plurals.export_summary, state.orderCount, state.orderCount)
                    },
                style = MaterialTheme.typography.titleMedium,
            )
            Button(
                onClick = { onAction(ExportAction.OnSave) },
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(TsundokuIcons.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                }
                HorizontalSpacer(8.dp)
                Text(stringResource(Res.string.export_save))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormatPicker(
    selected: ExportFormat,
    onSelect: (ExportFormat) -> Unit,
) {
    val formats = ExportFormat.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        formats.forEachIndexed { index, format ->
            SegmentedButton(
                selected = format == selected,
                onClick = { onSelect(format) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = formats.size),
            ) {
                Text(stringResource(format.labelRes))
            }
        }
    }
}

private val ExportFormat.labelRes: StringResource
    get() =
        when (this) {
            ExportFormat.CSV -> Res.string.export_format_csv
            ExportFormat.JSON -> Res.string.export_format_json
        }

private val ExportFormat.captionRes: StringResource
    get() =
        when (this) {
            ExportFormat.CSV -> Res.string.export_format_csv_caption
            ExportFormat.JSON -> Res.string.export_format_json_caption
        }

@PreviewThemes
@Composable
private fun ExportScreenPreview() {
    TsundokuTheme {
        Surface {
            ExportScreen(
                state = ExportState(isLoading = false, orderCount = 312, cancelledCount = 4),
                onAction = {},
            )
        }
    }
}
