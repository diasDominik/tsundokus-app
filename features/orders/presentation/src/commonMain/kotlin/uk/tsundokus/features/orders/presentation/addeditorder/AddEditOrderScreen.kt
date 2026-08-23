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
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_add
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_author_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_author_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_currency_option
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_delayed_to_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_delete
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_eta_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_order_date_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_order_date_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_price_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_price_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_publisher_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_publisher_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_received_date_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_release_date_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_save_changes
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_section_currency
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_section_reading
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_section_status
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_ship_date_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_store_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_store_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_title_error_required
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_title_label
import tsundokuapp.features.orders.presentation.generated.resources.add_edit_order_volume_label
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
import uk.tsundokus.features.orders.presentation.components.labelRes

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
        SectionLabel(Res.string.add_edit_order_section_status)
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OrderStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.status == status,
                    onClick = { onAction(AddEditOrderAction.OnStatusSelected(status)) },
                    label = { Text(stringResource(status.labelRes)) },
                )
            }
        }

        FormField(
            value = state.title,
            onValueChange = { onAction(AddEditOrderAction.OnTitleChange(it)) },
            label = Res.string.add_edit_order_title_label,
            isError = OrderFormField.TITLE in state.errors,
            supportingText = state.errorFor(OrderFormField.TITLE),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                value = state.author,
                onValueChange = { onAction(AddEditOrderAction.OnAuthorChange(it)) },
                label = Res.string.add_edit_order_author_label,
                isError = OrderFormField.AUTHOR in state.errors,
                supportingText = state.errorFor(OrderFormField.AUTHOR),
                modifier = Modifier.weight(1f),
            )
            FormField(
                value = state.volume,
                onValueChange = { onAction(AddEditOrderAction.OnVolumeChange(it)) },
                label = Res.string.add_edit_order_volume_label,
                modifier = Modifier.weight(1f),
            )
        }

        FormField(
            value = state.publisher,
            onValueChange = { onAction(AddEditOrderAction.OnPublisherChange(it)) },
            label = Res.string.add_edit_order_publisher_label,
            isError = OrderFormField.PUBLISHER in state.errors,
            supportingText = state.errorFor(OrderFormField.PUBLISHER),
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FormField(
                value = state.store,
                onValueChange = { onAction(AddEditOrderAction.OnStoreChange(it)) },
                label = Res.string.add_edit_order_store_label,
                isError = OrderFormField.STORE in state.errors,
                supportingText = state.errorFor(OrderFormField.STORE),
                modifier = Modifier.weight(1f),
            )
            FormField(
                value = state.price,
                onValueChange = { onAction(AddEditOrderAction.OnPriceChange(it)) },
                label = Res.string.add_edit_order_price_label,
                isError = OrderFormField.PRICE in state.errors,
                supportingText = state.errorFor(OrderFormField.PRICE),
                keyboardType = KeyboardType.Decimal,
                modifier = Modifier.weight(1f),
            )
        }

        SectionLabel(Res.string.add_edit_order_section_currency)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            AppCurrency.entries.forEachIndexed { index, currency ->
                SegmentedButton(
                    selected = state.currency == currency,
                    onClick = { onAction(AddEditOrderAction.OnCurrencySelected(currency)) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = AppCurrency.entries.size),
                ) {
                    Text(stringResource(Res.string.add_edit_order_currency_option, currency.symbol, currency.name))
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OrderDateField(
                value = state.orderDate,
                onValueChange = { onAction(AddEditOrderAction.OnOrderDateChange(it)) },
                label = Res.string.add_edit_order_order_date_label,
                isError = OrderFormField.ORDER_DATE in state.errors,
                supportingText = state.errorFor(OrderFormField.ORDER_DATE),
                modifier = Modifier.weight(1f),
            )
            OrderDateField(
                value = state.releaseDate,
                onValueChange = { onAction(AddEditOrderAction.OnReleaseDateChange(it)) },
                label = Res.string.add_edit_order_release_date_label,
                modifier = Modifier.weight(1f),
            )
        }

        if (state.status == OrderStatus.SHIPPED) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OrderDateField(
                    value = state.shipDate,
                    onValueChange = { onAction(AddEditOrderAction.OnShipDateChange(it)) },
                    label = Res.string.add_edit_order_ship_date_label,
                    modifier = Modifier.weight(1f),
                )
                OrderDateField(
                    value = state.eta,
                    onValueChange = { onAction(AddEditOrderAction.OnEtaChange(it)) },
                    label = Res.string.add_edit_order_eta_label,
                    modifier = Modifier.weight(1f),
                )
            }
        }

        if (state.status == OrderStatus.DELAYED) {
            OrderDateField(
                value = state.delayedTo,
                onValueChange = { onAction(AddEditOrderAction.OnDelayedToChange(it)) },
                label = Res.string.add_edit_order_delayed_to_label,
            )
        }

        if (state.status == OrderStatus.RECEIVED) {
            OrderDateField(
                value = state.receivedDate,
                onValueChange = { onAction(AddEditOrderAction.OnReceivedDateChange(it)) },
                label = Res.string.add_edit_order_received_date_label,
            )
        }

        SectionLabel(Res.string.add_edit_order_section_reading)
        ReadStateSegmented(
            selected = state.readState,
            onSelect = { onAction(AddEditOrderAction.OnReadStateSelected(it)) },
        )

        VerticalSpacer(8.dp)
        TsundokuButton(
            text =
                stringResource(
                    if (state.isEdit) {
                        Res.string.add_edit_order_save_changes
                    } else {
                        Res.string.add_edit_order_add
                    },
                ),
            onClick = { onAction(AddEditOrderAction.OnSave) },
            isLoading = state.isSaving,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isEdit) {
            VerticalSpacer(8.dp)
            TsundokuButton(
                text = stringResource(Res.string.add_edit_order_delete),
                onClick = { onAction(AddEditOrderAction.OnDelete) },
                style = TsundokuButtonStyle.DestructiveSecondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The message for a field, or null when it currently has no error. */
@Composable
private fun AddEditOrderState.errorFor(field: OrderFormField): String? =
    if (field !in errors) {
        null
    } else {
        stringResource(
            when (field) {
                OrderFormField.TITLE -> Res.string.add_edit_order_title_error_required
                OrderFormField.AUTHOR -> Res.string.add_edit_order_author_error_required
                OrderFormField.PUBLISHER -> Res.string.add_edit_order_publisher_error_required
                OrderFormField.STORE -> Res.string.add_edit_order_store_error_required
                OrderFormField.PRICE -> Res.string.add_edit_order_price_error_required
                OrderFormField.ORDER_DATE -> Res.string.add_edit_order_order_date_error_required
            },
        )
    }

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(label)) },
        isError = isError,
        supportingText = supportingText?.let { message -> { Text(message) } },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionLabel(text: StringResource) {
    Text(
        text = stringResource(text),
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
