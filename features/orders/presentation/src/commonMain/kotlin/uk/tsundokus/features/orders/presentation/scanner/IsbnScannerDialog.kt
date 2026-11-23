package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_cancel
import tsundokuapp.features.orders.presentation.generated.resources.scan_to_receive_hint
import tsundokuapp.features.orders.presentation.generated.resources.scanner_dialog_title
import tsundokuapp.features.orders.presentation.generated.resources.scanner_mode_barcode
import tsundokuapp.features.orders.presentation.generated.resources.scanner_mode_text
import tsundokuapp.features.orders.presentation.generated.resources.scanner_text_dialog_title
import tsundokuapp.features.orders.presentation.generated.resources.scanner_text_hint
import uk.tsundokus.core.designsystem.icon.TsundokuIcons

/**
 * Reads one ISBN and hands it back — for filling in the order form. Off the barcode it goes to
 * [onIsbn]; where [onIsbnRead] is given and the platform reads text, the printed ISBN can be read
 * instead (on a copyright page, or on a screen showing the order) and goes there.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IsbnScannerDialog(
    onIsbn: (isbn: String) -> Unit,
    onDismiss: () -> Unit,
    onIsbnRead: ((isbn: String) -> Unit)? = null,
) {
    val canReadText = onIsbnRead != null && rememberImageTextReader() != null
    var mode by rememberSaveable { mutableStateOf(IsbnCameraMode.BARCODE) }
    val readingText = canReadText && mode == IsbnCameraMode.TEXT

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.padding(16.dp).widthIn(max = 480.dp).fillMaxWidth(),
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val title =
                        if (readingText) Res.string.scanner_text_dialog_title else Res.string.scanner_dialog_title
                    Text(
                        text = stringResource(title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = TsundokuIcons.Close,
                            contentDescription = stringResource(Res.string.scan_to_receive_cancel),
                        )
                    }
                }
                if (canReadText) {
                    val modes = IsbnCameraMode.entries
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                        modes.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = option == mode,
                                onClick = { mode = option },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                            ) {
                                Text(
                                    stringResource(
                                        when (option) {
                                            IsbnCameraMode.BARCODE -> Res.string.scanner_mode_barcode
                                            IsbnCameraMode.TEXT -> Res.string.scanner_mode_text
                                        },
                                    ),
                                )
                            }
                        }
                    }
                }
                IsbnScanner(
                    onIsbn = { isbn -> if (readingText) onIsbnRead(isbn) else onIsbn(isbn) },
                    mode = if (readingText) IsbnCameraMode.TEXT else IsbnCameraMode.BARCODE,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                )
                Text(
                    text =
                        stringResource(
                            if (readingText) Res.string.scanner_text_hint else Res.string.scan_to_receive_hint,
                        ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
