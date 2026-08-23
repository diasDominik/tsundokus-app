package uk.tsundokus.core.designsystem.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle

/**
 * A two-button confirmation dialog for an action the user cannot walk back — deleting a record,
 * discarding unsaved edits.
 *
 * Copy is passed in rather than resolved here: `:core:designsystem` carries no strings, so the
 * calling feature owns (and translates) its own wording.
 *
 * @param isDestructive drives the confirm button's colour. Pass `true` when confirming destroys
 *   data, so the button reads as a warning rather than a plain default.
 */
@Composable
fun TsundokuConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isDestructive: Boolean = false,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title, style = MaterialTheme.typography.titleMedium) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            TsundokuButton(
                text = confirmText,
                onClick = onConfirm,
                style =
                    if (isDestructive) {
                        TsundokuButtonStyle.DestructivePrimary
                    } else {
                        TsundokuButtonStyle.Primary
                    },
            )
        },
        dismissButton = {
            TsundokuButton(
                text = dismissText,
                onClick = onDismiss,
                style = TsundokuButtonStyle.Text,
            )
        },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
