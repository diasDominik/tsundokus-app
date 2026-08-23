package uk.tsundokus.features.orders.presentation.addeditorder

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.components.OrderDateField
import uk.tsundokus.features.orders.presentation.components.ReadStateSegmented

@Composable
fun AddEditOrderRoot(
    orderId: String?,
    onSaved: () -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: AddEditOrderViewModel =
        koinViewModel(
            key = orderId ?: "add",
            parameters = { parametersOf(orderId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is AddEditOrderEvent.Saved -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
                onSaved()
            }

            is AddEditOrderEvent.Deleted -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
                onSaved()
            }

            is AddEditOrderEvent.ShowError -> {
                snackbarHostState.showSnackbar(event.message.asStringAsync())
            }
        }
    }

    AddEditOrderScreen(
        state = state,
        onAction = viewModel::onAction,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddEditOrderScreen(
    state: AddEditOrderState,
    onAction: (AddEditOrderAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionLabel("Status")
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = { onAction(AddEditOrderAction.OnStatusSelected(status)) },
                    label = { Text(status.label) },
                )
            }
        }

        FormField(
            value = state.title,
            onValueChange = { onAction(AddEditOrderAction.OnTitleChange(it)) },
            label = "Title",
            isError = state.titleError,
            supportingText = if (state.titleError) "Title is required" else null,
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                value = state.author,
                onValueChange = { onAction(AddEditOrderAction.OnAuthorChange(it)) },
                label = "Author",
                modifier = Modifier.weight(1f),
            )
            FormField(
                value = state.volume,
                onValueChange = { onAction(AddEditOrderAction.OnVolumeChange(it)) },
                label = "Volume",
                modifier = Modifier.weight(1f),
            )
        }

        FormField(
            value = state.publisher,
            onValueChange = { onAction(AddEditOrderAction.OnPublisherChange(it)) },
            label = "Publisher",
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                value = state.store,
                onValueChange = { onAction(AddEditOrderAction.OnStoreChange(it)) },
                label = "Store",
                modifier = Modifier.weight(1f),
            )
            FormField(
                value = state.price,
                onValueChange = { onAction(AddEditOrderAction.OnPriceChange(it)) },
                label = "Price",
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        SectionLabel("Currency")
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AppCurrency.entries.forEachIndexed { index, currency ->
                SegmentedButton(
                    selected = state.currency == currency,
                    onClick = { onAction(AddEditOrderAction.OnCurrencySelected(currency)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AppCurrency.entries.size),
                ) {
                    Text("${currency.symbol} ${currency.name}")
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OrderDateField(
                value = state.orderDate,
                onValueChange = { onAction(AddEditOrderAction.OnOrderDateChange(it)) },
                label = "Order date",
                modifier = Modifier.weight(1f),
            )
            OrderDateField(
                value = state.releaseDate,
                onValueChange = { onAction(AddEditOrderAction.OnReleaseDateChange(it)) },
                label = "Release date",
                modifier = Modifier.weight(1f),
            )
        }

        if (state.status == OrderStatus.SHIPPED) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OrderDateField(
                    value = state.shipDate,
                    onValueChange = { onAction(AddEditOrderAction.OnShipDateChange(it)) },
                    label = "Ship date",
                    modifier = Modifier.weight(1f),
                )
                OrderDateField(
                    value = state.eta,
                    onValueChange = { onAction(AddEditOrderAction.OnEtaChange(it)) },
                    label = "ETA",
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.status == OrderStatus.DELAYED) {
            OrderDateField(
                value = state.delayedTo,
                onValueChange = { onAction(AddEditOrderAction.OnDelayedToChange(it)) },
                label = "Delayed to",
            )
        }

        if (state.status == OrderStatus.RECEIVED) {
            OrderDateField(
                value = state.receivedDate,
                onValueChange = { onAction(AddEditOrderAction.OnReceivedDateChange(it)) },
                label = "Received date",
            )
        }

        SectionLabel("Reading")
        ReadStateSegmented(
            selected = state.readState,
            onSelect = { onAction(AddEditOrderAction.OnReadStateSelected(it)) },
        )

        VerticalSpacer(8.dp)
        TsundokuButton(
            text = if (state.isEdit) "Save changes" else "Add order",
            onClick = { onAction(AddEditOrderAction.OnSave) },
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isEdit) {
            VerticalSpacer(8.dp)
            TsundokuButton(
                text = "Delete order",
                onClick = { onAction(AddEditOrderAction.OnDelete) },
                style = TsundokuButtonStyle.DestructiveSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@PreviewThemes
@Composable
private fun AddEditOrderScreenPreview() {
    TsundokuTheme {
        Surface {
            AddEditOrderScreen(
                state =
                    AddEditOrderState(
                        title = "Chainsaw Man",
                        author = "Tatsuki Fujimoto",
                        volume = "Vol. 12",
                        store = "Amazon",
                        price = "12.99",
                        status = OrderStatus.SHIPPED,
                        eta = "2026-07-02",
                    ),
                onAction = {},
            )
        }
    }
}
