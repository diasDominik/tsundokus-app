package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer

/** Group header used by the orders/reading lists: optional dot + label + count. */
@Composable
fun SectionHeader(
    label: String,
    count: Int,
    modifier: Modifier = Modifier,
    dotColor: Color? = null,
) {
    Row(
        modifier = modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dotColor != null) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(dotColor, CircleShape),
            )
            HorizontalSpacer(8.dp)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        HorizontalSpacer(6.dp)
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
