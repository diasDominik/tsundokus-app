package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import uk.tsundokus.features.orders.domain.models.ReadState

/** Segmented control over the [ReadState] values (Want / Reading / Read). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadStateSegmented(
    selected: ReadState,
    onSelect: (ReadState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = ReadState.entries
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, readState ->
            SegmentedButton(
                selected = readState == selected,
                onClick = { onSelect(readState) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(stringResource(readState.fullLabelRes))
            }
        }
    }
}
