package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/** Leading icon tile of a row: the status icon inside a rounded square filled with its color. */
@Composable
fun StatusTile(
    status: OrderStatus,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .background(status.containerColor(), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = status.icon(),
            contentDescription = status.label,
            tint = status.onContainerColor(),
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** A labelled status pill (icon + label) used in the detail header. */
@Composable
fun StatusChip(
    status: OrderStatus,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .background(status.containerColor(), RoundedCornerShape(50))
                .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = status.icon(),
            contentDescription = null,
            tint = status.onContainerColor(),
            modifier = Modifier.size(14.dp),
        )
        HorizontalSpacer(6.dp)
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelMedium,
            color = status.onContainerColor(),
        )
    }
}

/** Small read-state badge shown on rows. */
@Composable
fun ReadStateBadge(
    readState: ReadState,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .background(readState.containerColor(), RoundedCornerShape(50))
                .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = readState.label,
            style = MaterialTheme.typography.labelSmall,
            color = readState.onContainerColor(),
        )
    }
}
