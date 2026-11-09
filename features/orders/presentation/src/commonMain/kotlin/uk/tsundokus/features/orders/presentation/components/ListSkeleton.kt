package uk.tsundokus.features.orders.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.list_loading_cd
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme

private val ROW_HEIGHT = 72.dp
private val ROW_SPACING = 8.dp

/**
 * Placeholder rows shaped like the list's cards, shown until the local database has answered. A blank
 * body would leave the screen looking broken, and the empty state would claim there is nothing; rows in
 * the right place say "coming" and let the real list land without the layout jumping. As many rows as
 * fit the space, so the placeholder never ends halfway down the screen.
 */
@Composable
fun ListSkeleton(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
) {
    val pulse by rememberInfiniteTransition(label = "skeleton").animateFloat(
        initialValue = 1f,
        targetValue = 0.45f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "skeleton-alpha",
    )
    val loading = stringResource(Res.string.list_loading_cd)
    BoxWithConstraints(
        modifier = modifier.clipToBounds().semantics { contentDescription = loading },
    ) {
        val rows = (maxHeight / (ROW_HEIGHT + ROW_SPACING)).toInt() + 1
        Column(
            modifier = Modifier.padding(contentPadding).alpha(pulse),
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING),
        ) {
            repeat(rows) { SkeletonRow() }
        }
    }
}

@Composable
private fun SkeletonRow() {
    val shape = RoundedCornerShape(16.dp)
    val block = MaterialTheme.colorScheme.surfaceContainerHighest
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, shape)
                .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).background(block, RoundedCornerShape(12.dp)))
        HorizontalSpacer(12.dp)
        Column(modifier = Modifier.weight(1f)) {
            Box(Modifier.fillMaxWidth(0.6f).height(14.dp).background(block, RoundedCornerShape(4.dp)))
            VerticalSpacer(8.dp)
            Box(Modifier.fillMaxWidth(0.4f).height(10.dp).background(block, RoundedCornerShape(4.dp)))
        }
        HorizontalSpacer(8.dp)
        Box(Modifier.size(width = 52.dp, height = 26.dp).background(block, RoundedCornerShape(50)))
    }
}

@PreviewThemes
@Composable
private fun ListSkeletonPreview() {
    TsundokuTheme {
        Surface {
            ListSkeleton()
        }
    }
}
