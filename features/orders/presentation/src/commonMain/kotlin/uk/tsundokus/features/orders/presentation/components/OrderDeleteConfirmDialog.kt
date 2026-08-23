package uk.tsundokus.features.orders.presentation.components

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_cancel
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_confirm
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_message
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_message_untitled
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_title
import uk.tsundokus.core.designsystem.dialog.TsundokuConfirmDialog

/**
 * Guards the one action in the app that destroys data. Deleting an order is not undoable from the
 * UI, so it asks first — from wherever it is offered (order detail, and the edit form).
 *
 * @param orderTitle named back to the user so they can see *which* order they are about to lose;
 *   a blank title (an order saved before the title became required) falls back to generic copy.
 */
@Composable
fun OrderDeleteConfirmDialog(
    orderTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TsundokuConfirmDialog(
        title = stringResource(Res.string.order_delete_confirm_title),
        message =
            if (orderTitle.isBlank()) {
                stringResource(Res.string.order_delete_confirm_message_untitled)
            } else {
                stringResource(Res.string.order_delete_confirm_message, orderTitle)
            },
        confirmText = stringResource(Res.string.order_delete_confirm_confirm),
        dismissText = stringResource(Res.string.order_delete_confirm_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
    )
}
