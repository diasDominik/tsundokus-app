package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** How long a scrolling title rests at its start, readable, before (and between) each pass. */
private const val MARQUEE_REST_MILLIS = 3_000

/** How far the closing fade reaches into the text itself, beyond the gap, while it overflows. */
private val END_FADE_INTO_TEXT = 20.dp

/**
 * A one-line title that scrolls when it is too long, fading out at its edges instead of being cut.
 * A plain marquee clips the text hard, so a glance mid-pass reads as a broken word ("erk Deluxe
 * Edition"); the fades make it read as text sliding past.
 *
 * The opening fade is drawn in [startGap], empty space the caller already leaves before the title, so
 * at rest the first letters are crisp and aligned with the lines below. The closing fade is drawn over
 * the title's own last stretch instead, never past its end: whatever sits next to it (a price) keeps
 * its clear gap, and the end of an overflowing title, cut off anyway, trails away before reaching it.
 * Only an overflowing title fades at all; a title that fits is drawn untouched.
 */
@Composable
fun MarqueeTitle(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    startGap: Dp = 12.dp,
) {
    // The marquee lays the text out at its full length, so this is the whole title's width.
    var textWidth by remember { mutableIntStateOf(0) }
    Text(
        text = text,
        style = style,
        fontWeight = fontWeight,
        maxLines = 1,
        onTextLayout = { textWidth = it.size.width },
        modifier = modifier.fadingMarquee(startGap, endGap = 0.dp, textWidth = { textWidth }),
    )
}

private fun Modifier.fadingMarquee(
    startGap: Dp,
    endGap: Dp,
    textWidth: () -> Int,
): Modifier =
    this
        // Reach into the gaps: measure wider than given, report the given width, shift left.
        .layout { measurable, constraints ->
            val start = startGap.roundToPx()
            val end = endGap.roundToPx()
            val wider =
                if (constraints.hasBoundedWidth) {
                    constraints.copy(
                        minWidth = constraints.minWidth + start + end,
                        maxWidth = constraints.maxWidth + start + end,
                    )
                } else {
                    constraints
                }
            val placeable = measurable.measure(wider)
            layout((placeable.width - start - end).coerceAtLeast(0), placeable.height) {
                placeable.place(-start, 0)
            }
        }
        // Offscreen so the DstIn gradient masks only this text, not what is behind it.
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val start = startGap.toPx()
            val end = endGap.toPx()
            val overflows = textWidth() > size.width - start - end
            if (!overflows) return@drawWithContent
            val endFade = end + END_FADE_INTO_TEXT.toPx()
            drawRect(
                brush =
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        start / size.width to Color.Black,
                        1f - endFade / size.width to Color.Black,
                        1f to Color.Transparent,
                    ),
                blendMode = BlendMode.DstIn,
            )
        }.basicMarquee(initialDelayMillis = MARQUEE_REST_MILLIS, repeatDelayMillis = MARQUEE_REST_MILLIS)
        .padding(start = startGap, end = endGap)
