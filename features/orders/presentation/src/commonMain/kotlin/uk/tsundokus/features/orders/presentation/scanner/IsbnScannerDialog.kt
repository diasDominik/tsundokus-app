package uk.tsundokus.features.orders.presentation.scanner

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import uk.tsundokus.core.designsystem.icon.TsundokuIcons

/** Reads one ISBN and hands it back — for filling in the order form. */
@Composable
fun IsbnScannerDialog(
    onIsbn: (isbn: String) -> Unit,
    onDismiss: () -> Unit,
) {
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
                    Text(
                        text = stringResource(Res.string.scanner_dialog_title),
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
                IsbnScanner(
                    onIsbn = onIsbn,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                )
                Text(
                    text = stringResource(Res.string.scan_to_receive_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
