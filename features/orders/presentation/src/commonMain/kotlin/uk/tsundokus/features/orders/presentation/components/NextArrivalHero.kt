package uk.tsundokus.features.orders.presentation.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus

/** "Next arrival" hero card shown at the top of the orders list. */
@Composable
fun NextArrivalHero(
    order: Order,
    today: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor = order.status.containerColor(),
                contentColor = order.status.onContainerColor(),
            ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusTile(status = order.status)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = heroTitle(order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = heroSubtitle(order, today),
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun heroTitle(order: Order): String =
    listOf(order.title, order.volume).filter { it.isNotBlank() }.joinToString(" ")

private fun heroSubtitle(
    order: Order,
    today: String,
): String {
    val prefix = if (order.status == OrderStatus.ORDERED) "Releases " else "Expected "
    val date = fmtDate(arrivalDate(order, today).orEmpty())
    return listOf("$prefix$date", order.store).filter { it.isNotBlank() }.joinToString(" · ")
}
