package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * A single line of chips that scrolls sideways when they do not fit. A scrolling row gives no sign
 * of it on its own — chips simply run off the edge, and the last ones look like they do not exist —
 * so whichever side has more to reach fades out.
 */
@Composable
internal fun FadingChipRow(
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable RowScope.() -> Unit,
) {
    val fadeColor = MaterialTheme.colorScheme.background
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        // matchParentSize, not fillMaxHeight: the fades must take their height *from* the chip row
        // without contributing to layout. Filling height made this Box claim the whole column and
        // left a list below it zero pixels tall.
        Box(modifier = Modifier.matchParentSize()) {
            EdgeFade(Alignment.CenterStart, fadeColor, visible = scrollState.value > 0)
            EdgeFade(Alignment.CenterEnd, fadeColor, visible = scrollState.value < scrollState.maxValue)
        }
    }
}

/** A short gradient over the scrolling edge, purely a hint that the row continues. */
@Composable
private fun BoxScope.EdgeFade(
    alignment: Alignment,
    color: Color,
    visible: Boolean,
) {
    if (!visible) return
    val colors =
        if (alignment == Alignment.CenterStart) {
            listOf(color, Color.Transparent)
        } else {
            listOf(Color.Transparent, color)
        }
    Box(
        modifier =
            Modifier
                .align(alignment)
                .width(24.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(colors)),
    )
}
