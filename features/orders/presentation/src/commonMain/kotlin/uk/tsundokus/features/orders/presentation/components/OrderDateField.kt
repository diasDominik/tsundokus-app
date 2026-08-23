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
    label: String,
    modifier: Modifier = Modifier,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    // A read-only text field swallows clicks, so the press interaction is what opens the picker.
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) showPicker = true
        }
    }

    OutlinedTextField(
        value = fmtDate(value),
        onValueChange = {},
        label = { Text(label) },
        placeholder = { Text("Select date") },
        readOnly = true,
        singleLine = true,
        interactionSource = interactionSource,
        trailingIcon = {
            if (value.isBlank()) {
                Icon(
                    imageVector = TsundokuIcons.CalendarToday,
                    contentDescription = "Pick $label",
                )
            } else {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = TsundokuIcons.Close,
                        contentDescription = "Clear $label",
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
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
