package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer

/** Visual state of a single timeline node. */
enum class TimelineNodeState {
    DONE,
    CURRENT,
    TODO,
    CANCEL,
    DELAY,
}

/**
 * One step in the order detail timeline. [date] is already display-formatted ("—" when unset).
 *
 * [isDateKnown] is what the dot is drawn from: a step the order has passed but carries no date for
 * is drawn hollow, so "shipped on the 3rd" and "shipped, date unknown" don't look identical.
 */
data class TimelineNode(
    val label: StringResource,
    val date: String,
    val state: TimelineNodeState,
    val isDateKnown: Boolean = true,
)

/** Vertical status timeline shown on the order detail screen. */
@Composable
fun StatusTimeline(
    nodes: List<TimelineNode>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        nodes.forEachIndexed { index, node ->
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val dotColor = node.state.color()
                    Box(
                        modifier =
                            Modifier
                                .size(12.dp)
                                .then(
                                    if (node.isDateKnown) {
                                        Modifier.background(dotColor, CircleShape)
                                    } else {
                                        Modifier.border(2.dp, dotColor, CircleShape)
                                    },
                                ),
                    )
                    if (index != nodes.lastIndex) {
                        Box(
                            modifier =
                                Modifier
                                    .width(2.dp)
                                    .height(28.dp)
                                    .background(MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                HorizontalSpacer(12.dp)
                Column(modifier = Modifier.padding(bottom = if (index == nodes.lastIndex) 0.dp else 16.dp)) {
                    Text(
                        text = stringResource(node.label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight =
                            if (node.state ==
                                TimelineNodeState.CURRENT
                            ) {
                                FontWeight.SemiBold
                            } else {
                                FontWeight.Normal
                            },
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (node.date.isNotBlank()) {
                        Text(
                            text = node.date,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
@ReadOnlyComposable
private fun TimelineNodeState.color(): Color =
    when (this) {
        TimelineNodeState.DONE -> MaterialTheme.colorScheme.primary
        TimelineNodeState.CURRENT -> MaterialTheme.colorScheme.tertiary
        TimelineNodeState.TODO -> MaterialTheme.colorScheme.outlineVariant
        TimelineNodeState.CANCEL -> MaterialTheme.colorScheme.error
        TimelineNodeState.DELAY -> MaterialTheme.colorScheme.error
    }
