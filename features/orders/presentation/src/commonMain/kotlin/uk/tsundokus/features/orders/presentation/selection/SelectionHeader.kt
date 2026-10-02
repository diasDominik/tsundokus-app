package uk.tsundokus.features.orders.presentation.selection

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_cancel
import tsundokuapp.features.orders.presentation.generated.resources.order_delete_confirm_confirm
import tsundokuapp.features.orders.presentation.generated.resources.selection_close_cd
import tsundokuapp.features.orders.presentation.generated.resources.selection_count
import tsundokuapp.features.orders.presentation.generated.resources.selection_delete_confirm_message
import tsundokuapp.features.orders.presentation.generated.resources.selection_delete_confirm_title
import tsundokuapp.features.orders.presentation.generated.resources.selection_deleted
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_read
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_reading
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_received
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_shipped
import tsundokuapp.features.orders.presentation.generated.resources.selection_marked_want
import tsundokuapp.features.orders.presentation.generated.resources.selection_read_state
import tsundokuapp.features.orders.presentation.generated.resources.selection_select_all
import uk.tsundokus.core.designsystem.dialog.TsundokuConfirmDialog
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.components.fullLabelRes

/**
 * Takes the place of a list's title row while orders are picked: leave, how many, pick everything
 * shown, then the list's own [actions].
 */
@Composable
internal fun SelectionHeader(
    count: Int,
    onClose: () -> Unit,
    onSelectAll: () -> Unit,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onClose) {
            Icon(TsundokuIcons.Close, contentDescription = stringResource(Res.string.selection_close_cd))
        }
        Text(
            text = pluralStringResource(Res.plurals.selection_count, count, count),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onSelectAll) {
            Icon(TsundokuIcons.SelectAll, contentDescription = stringResource(Res.string.selection_select_all))
        }
        actions()
    }
}

/** A button that opens the three reading states and hands back the one picked. */
@Composable
internal fun ReadStateMenuButton(
    onSelect: (ReadState) -> Unit,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    IconButton(onClick = { expanded = true }, enabled = enabled) {
        Icon(TsundokuIcons.MenuBook, contentDescription = stringResource(Res.string.selection_read_state))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        ReadState.groupOrder.forEach { readState ->
            DropdownMenuItem(
                text = { Text(stringResource(readState.fullLabelRes)) },
                onClick = {
                    expanded = false
                    onSelect(readState)
                },
            )
        }
    }
}

@Composable
internal fun DeleteSelectedConfirmDialog(
    count: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    TsundokuConfirmDialog(
        title = pluralStringResource(Res.plurals.selection_delete_confirm_title, count, count),
        message = stringResource(Res.string.selection_delete_confirm_message),
        confirmText = stringResource(Res.string.order_delete_confirm_confirm),
        dismissText = stringResource(Res.string.order_delete_confirm_cancel),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        isDestructive = true,
    )
}

/**
 * Back leaves selection mode instead of the screen while [isSelecting]. Android's system back and
 * gesture reach it; desktop and web leave it with Escape, which each list handles itself.
 */
@Composable
internal fun SelectionBackHandler(
    isSelecting: Boolean,
    onClear: () -> Unit,
) {
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isSelecting,
        onBackCompleted = onClear,
    )
}

/** What the snackbar says once [count] orders took [this] change. */
internal fun SelectionChange.doneMessage(count: Int): UiText =
    when (this) {
        is SelectionChange.Status -> {
            when (status) {
                OrderStatus.RECEIVED -> {
                    UiText.PluralResource(
                        Res.plurals.selection_marked_received,
                        count,
                        arrayOf(count),
                    )
                }

                OrderStatus.SHIPPED -> {
                    UiText.PluralResource(
                        Res.plurals.selection_marked_shipped,
                        count,
                        arrayOf(count),
                    )
                }

                OrderStatus.CANCELLED -> {
                    UiText.PluralResource(
                        Res.plurals.selection_marked_cancelled,
                        count,
                        arrayOf(count),
                    )
                }

                // Not offered: a delay needs a date, and nothing goes back to ordered in bulk.
                OrderStatus.ORDERED, OrderStatus.DELAYED -> {
                    error("No bulk change to $status")
                }
            }
        }

        is SelectionChange.Reading -> {
            when (readState) {
                ReadState.WANT -> {
                    UiText.PluralResource(Res.plurals.selection_marked_want, count, arrayOf(count))
                }

                ReadState.READING -> {
                    UiText.PluralResource(
                        Res.plurals.selection_marked_reading,
                        count,
                        arrayOf(count),
                    )
                }

                ReadState.READ -> {
                    UiText.PluralResource(Res.plurals.selection_marked_read, count, arrayOf(count))
                }
            }
        }
    }

internal fun deletedMessage(count: Int): UiText =
    UiText.PluralResource(Res.plurals.selection_deleted, count, arrayOf(count))

@PreviewThemes
@Composable
private fun SelectionHeaderPreview() {
    TsundokuTheme {
        Surface {
            SelectionHeader(count = 3, onClose = {}, onSelectAll = {}) {
                ReadStateMenuButton(onSelect = {})
            }
        }
    }
}
