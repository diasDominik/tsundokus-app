package uk.tsundokus.features.orders.presentation.scantoreceive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_add_new
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_already_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_already_received
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_cancel
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_hint
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_invalid
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_manual_label
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_manual_submit
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_no_camera
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_pick_duplicate_message
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_pick_duplicate_title
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_pick_unknown_empty
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_pick_unknown_message
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_pick_unknown_title
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_received
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_undo
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_undone
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.components.OrderRow
import uk.tsundokus.features.orders.presentation.components.fmtDate
import uk.tsundokus.features.orders.presentation.components.todayIso
import uk.tsundokus.features.orders.presentation.scanner.IsbnScanner
import uk.tsundokus.features.orders.presentation.scanner.isCameraScanningSupported

@Composable
fun ScanToReceiveRoot(
    onOpenOrder: (String) -> Unit,
    onAddOrder: (isbn: String) -> Unit,
    snackbar: SnackbarController,
    viewModel: ScanToReceiveViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ScanToReceiveEvent.AddOrder -> onAddOrder(event.isbn)
            is ScanToReceiveEvent.ShowError -> snackbar.show(event.message)
        }
    }

    ScanToReceiveScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenOrder = onOpenOrder,
        cameraSupported = remember { isCameraScanningSupported() },
    )
}

@Composable
private fun ScanToReceiveScreen(
    state: ScanToReceiveState,
    onAction: (ScanToReceiveAction) -> Unit,
    onOpenOrder: (String) -> Unit,
    cameraSupported: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = MAX_WIDTH).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (cameraSupported) {
                Viewfinder(state = state, onAction = onAction)
            }
            val hint =
                if (cameraSupported) Res.string.scan_to_receive_hint else Res.string.scan_to_receive_no_camera
            Text(
                text = stringResource(hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ManualIsbnField(state = state, onAction = onAction, autoFocus = !cameraSupported)
            state.outcome?.let { outcome ->
                OutcomeCard(outcome = outcome, onAction = onAction, onOpenOrder = onOpenOrder)
            }
        }
    }

    state.picking?.let { pick ->
        OrderPickDialog(pick = pick, onAction = onAction)
    }
}

private val MAX_WIDTH = 600.dp

/**
 * The camera, switched off while a choice or a write is pending. Removing it rather than covering
 * it matters on web, where the preview is an HTML element that would sit above any dialog.
 */
@Composable
private fun Viewfinder(
    state: ScanToReceiveState,
    onAction: (ScanToReceiveAction) -> Unit,
) {
    val viewfinderModifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)
    if (state.isCameraActive) {
        IsbnScanner(
            onIsbn = { onAction(ScanToReceiveAction.OnIsbnScanned(it)) },
            ignoreIsbn = state.lastIsbn,
            modifier = viewfinderModifier,
        )
    } else {
        Box(
            modifier = viewfinderModifier.background(Color.Black, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (state.isBusy) CircularProgressIndicator(color = Color.White)
        }
    }
}

@Composable
private fun ManualIsbnField(
    state: ScanToReceiveState,
    onAction: (ScanToReceiveAction) -> Unit,
    autoFocus: Boolean,
) {
    val focusRequester = remember { FocusRequester() }
    // Without a camera, typing is the only way in — and a USB barcode scanner types into whatever
    // has focus, so the field should have it from the start.
    LaunchedEffect(autoFocus) {
        if (autoFocus) focusRequester.requestFocus()
    }
    val submit = { onAction(ScanToReceiveAction.OnManualIsbnSubmit) }
    OutlinedTextField(
        value = state.manualIsbn,
        onValueChange = { onAction(ScanToReceiveAction.OnManualIsbnChange(it)) },
        label = { Text(stringResource(Res.string.scan_to_receive_manual_label)) },
        isError = state.isManualIsbnInvalid,
        supportingText =
            if (state.isManualIsbnInvalid) {
                { Text(stringResource(Res.string.scan_to_receive_invalid)) }
            } else {
                null
            },
        trailingIcon = {
            TextButton(onClick = submit, enabled = state.manualIsbn.isNotEmpty()) {
                Text(stringResource(Res.string.scan_to_receive_manual_submit))
            }
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { submit() }),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                // A hardware Enter — typed, or sent by a barcode scanner after an ISBN-10.
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown &&
                        (event.key == Key.Enter || event.key == Key.NumPadEnter)
                    ) {
                        submit()
                        true
                    } else {
                        false
                    }
                },
    )
}

@Composable
private fun OutcomeCard(
    outcome: ScanOutcome,
    onAction: (ScanToReceiveAction) -> Unit,
    onOpenOrder: (String) -> Unit,
) {
    val (order, headline) =
        when (outcome) {
            is ScanOutcome.Received -> {
                outcome.order to stringResource(Res.string.scan_to_receive_received)
            }

            is ScanOutcome.Undone -> {
                outcome.order to stringResource(Res.string.scan_to_receive_undone)
            }

            is ScanOutcome.AlreadyReceived -> {
                val order = outcome.order
                order to
                    if (order.status == OrderStatus.CANCELLED) {
                        stringResource(Res.string.scan_to_receive_already_cancelled)
                    } else {
                        stringResource(Res.string.scan_to_receive_already_received, fmtDate(order.receivedDate))
                    }
            }
        }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (outcome is ScanOutcome.Received) {
                Icon(
                    imageVector = TsundokuIcons.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 8.dp),
                )
            }
            Text(
                text = headline,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (outcome is ScanOutcome.Received) {
                TextButton(onClick = { onAction(ScanToReceiveAction.OnUndo) }) {
                    Text(stringResource(Res.string.scan_to_receive_undo))
                }
            }
        }
        OrderRow(order = order, today = todayIso(), onClick = { onOpenOrder(order.id) })
    }
}

@Composable
private fun OrderPickDialog(
    pick: OrderPick,
    onAction: (ScanToReceiveAction) -> Unit,
) {
    val isUnknown = pick.reason == OrderPick.Reason.UNKNOWN
    val today = remember { todayIso() }
    AlertDialog(
        onDismissRequest = { onAction(ScanToReceiveAction.OnPickDismissed) },
        title = {
            Text(
                stringResource(
                    if (isUnknown) {
                        Res.string.scan_to_receive_pick_unknown_title
                    } else {
                        Res.string.scan_to_receive_pick_duplicate_title
                    },
                ),
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    when {
                        !isUnknown -> stringResource(Res.string.scan_to_receive_pick_duplicate_message, pick.isbn)
                        pick.orders.isEmpty() -> stringResource(Res.string.scan_to_receive_pick_unknown_empty)
                        else -> stringResource(Res.string.scan_to_receive_pick_unknown_message, pick.isbn)
                    },
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(pick.orders, key = Order::id) { order ->
                        OrderRow(
                            order = order,
                            today = today,
                            onClick = { onAction(ScanToReceiveAction.OnOrderPicked(order.id)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (isUnknown) {
                TextButton(onClick = { onAction(ScanToReceiveAction.OnAddAsNewOrder) }) {
                    Text(stringResource(Res.string.scan_to_receive_add_new))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ScanToReceiveAction.OnPickDismissed) }) {
                Text(stringResource(Res.string.scan_to_receive_cancel))
            }
        },
    )
}

@PreviewThemes
@Composable
private fun ScanToReceiveScreenPreview() {
    TsundokuTheme {
        Surface {
            val order =
                Order(
                    id = "1",
                    title = "Chainsaw Man",
                    author = "Tatsuki Fujimoto",
                    volume = "Vol. 12",
                    status = OrderStatus.RECEIVED,
                    isbn = "9784088820453",
                    receivedDate = "2026-09-29",
                )
            ScanToReceiveScreen(
                state =
                    ScanToReceiveState(
                        outcome = ScanOutcome.Received(order, previous = order.copy(status = OrderStatus.SHIPPED)),
                    ),
                onAction = {},
                onOpenOrder = {},
                cameraSupported = false,
            )
        }
    }
}
