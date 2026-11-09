package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.stats.ArrivalEstimate

/**
 * A single order row: status tile, then title and price, subtitle, date and read-state. Only the
 * price shares the title's line, so a long title keeps the room. [checked] is null outside selection
 * mode; in it, the row shows a checkbox and a tap toggles it. [onLongClick] starts selection mode.
 * [estimate] is shown, as an estimate, when a shipped order has no ETA of its own.
 */
@Composable
fun OrderRow(
    order: Order,
    today: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    checked: Boolean? = null,
    onLongClick: (() -> Unit)? = null,
    estimate: ArrivalEstimate? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    OutlinedCard(
        shape = shape,
        border =
            when {
                checked == true -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                selected -> BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            },
        modifier =
            modifier
                .fillMaxWidth()
                .clip(shape)
                .pickableRowClicks(checked = checked, onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checked != null) {
                Checkbox(checked = checked, onCheckedChange = null)
                HorizontalSpacer(8.dp)
            }
            OrderThumbnail(isbn = order.isbn, hasCover = order.hasCover, status = order.status)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MarqueeTitle(
                        text = order.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        startGap = 12.dp,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalSpacer(8.dp)
                    Text(
                        text = priceLabel(order),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (order.subtitle.isNotBlank()) {
                    Text(
                        text = order.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                VerticalSpacer(4.dp)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dateLabel(order, today, estimate),
                        style = MaterialTheme.typography.labelSmall,
                        color = order.status.accentColor(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    // "Want to read" is the default for every book not yet in hand, so on those rows it
                    // says nothing; it only means something once the book is on the pile.
                    if (order.readState != ReadState.WANT || order.status == OrderStatus.RECEIVED) {
                        HorizontalSpacer(8.dp)
                        ReadStateBadge(readState = order.readState)
                    }
                }
            }
        }
    }
}

/**
 * A list row's taps: [onClick] opens the row, or toggles it while the list is picking orders
 * ([checked] not null), where the row reads out as a checkbox. A long press runs [onLongClick].
 */
internal fun Modifier.pickableRowClicks(
    checked: Boolean?,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?,
): Modifier =
    combinedClickable(
        role = if (checked != null) Role.Checkbox else Role.Button,
        onLongClick = onLongClick,
        onClick = onClick,
    ).then(
        if (checked != null) Modifier.semantics { toggleableState = ToggleableState(checked) } else Modifier,
    )
