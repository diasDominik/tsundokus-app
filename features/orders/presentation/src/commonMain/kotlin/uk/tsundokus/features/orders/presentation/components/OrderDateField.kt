package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_date_field_cancel
import tsundokuapp.features.orders.presentation.generated.resources.order_date_field_clear_cd
import tsundokuapp.features.orders.presentation.generated.resources.order_date_field_confirm
import tsundokuapp.features.orders.presentation.generated.resources.order_date_field_pick_cd
import tsundokuapp.features.orders.presentation.generated.resources.order_date_field_placeholder
import uk.tsundokus.core.designsystem.icon.TsundokuIcons

/**
 * Date input backed by the Material date picker. The value in/out is an ISO `yyyy-MM-dd` string
 * (blank when unset); the field renders it as `d MMM yyyy` and is read-only, so a date can only be
 * set through the picker or cleared through the trailing action — it can never hold a malformed
 * value.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDateField(
    value: String,
    onValueChange: (String) -> Unit,
    label: StringResource,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val labelText = stringResource(label)

    // A read-only text field swallows clicks, so the press interaction is what opens the picker.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showPicker = true
        }
    }

    OutlinedTextField(
        value = fmtDate(value),
        onValueChange = {},
        label = { Text(labelText) },
        placeholder = { Text(stringResource(Res.string.order_date_field_placeholder)) },
        readOnly = true,
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = {
            if (value.isBlank()) {
                Icon(
                    imageVector = TsundokuIcons.CalendarToday,
                    contentDescription = stringResource(Res.string.order_date_field_pick_cd, labelText),
                )
            } else {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = TsundokuIcons.Close,
                        contentDescription = stringResource(Res.string.order_date_field_clear_cd, labelText),
                    )
                }
            }
        },
        modifier = modifier.fillMaxWidth(),
    )

    if (showPicker) {
        val pickerState =
            rememberDatePickerState(initialSelectedDateMillis = epochMillisFromIso(value))
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { onValueChange(isoFromEpochMillis(it)) }
                        showPicker = false
                    },
                    enabled = pickerState.selectedDateMillis != null,
                ) {
                    Text(stringResource(Res.string.order_date_field_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text(stringResource(Res.string.order_date_field_cancel))
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
