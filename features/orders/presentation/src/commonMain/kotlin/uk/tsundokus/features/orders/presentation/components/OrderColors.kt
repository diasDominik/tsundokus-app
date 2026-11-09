package uk.tsundokus.features.orders.presentation.components
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/**
 * Maps the order/reading domain enums onto Material 3 color roles and `material-icons-core`
 * icons. Loudness follows what needs attention: a delay is loudest, then a parcel on its way;
 * an order still waiting is warm but quiet, and a received or cancelled one recedes, its state
 * carried by the tinted icon rather than a filled colour. The reading states stay clear of the
 * status colours, so a row's two badges never look like one. Container/on-color pairs follow the design spec; icons fall back to the nearest icon
 * that ships in `material-icons-core` (the only icon dependency available).
 */

@Composable
@ReadOnlyComposable
fun OrderStatus.containerColor(): Color =
    when (this) {
        OrderStatus.ORDERED -> MaterialTheme.colorScheme.surfaceContainerHighest
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primaryContainer
        OrderStatus.DELAYED -> MaterialTheme.colorScheme.errorContainer
        OrderStatus.RECEIVED -> MaterialTheme.colorScheme.surfaceContainerHigh
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.surfaceContainer
    }

@Composable
@ReadOnlyComposable
fun OrderStatus.onContainerColor(): Color =
    when (this) {
        OrderStatus.ORDERED -> MaterialTheme.colorScheme.secondary
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.onPrimaryContainer
        OrderStatus.DELAYED -> MaterialTheme.colorScheme.onErrorContainer
        OrderStatus.RECEIVED -> MaterialTheme.colorScheme.tertiary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.outline
    }

/** Stronger accent role used to tint the small date label on a row. */
@Composable
@ReadOnlyComposable
fun OrderStatus.accentColor(): Color =
    when (this) {
        OrderStatus.ORDERED -> MaterialTheme.colorScheme.secondary
        OrderStatus.SHIPPED -> MaterialTheme.colorScheme.primary
        OrderStatus.DELAYED -> MaterialTheme.colorScheme.error
        OrderStatus.RECEIVED -> MaterialTheme.colorScheme.tertiary
        OrderStatus.CANCELLED -> MaterialTheme.colorScheme.onSurfaceVariant
    }

@Composable
@ReadOnlyComposable
fun ReadState.containerColor(): Color =
    when (this) {
        ReadState.WANT -> MaterialTheme.colorScheme.surfaceContainerHighest
        ReadState.READING -> MaterialTheme.colorScheme.tertiaryContainer
        ReadState.READ -> MaterialTheme.colorScheme.tertiary
    }

@Composable
@ReadOnlyComposable
fun ReadState.onContainerColor(): Color =
    when (this) {
        ReadState.WANT -> MaterialTheme.colorScheme.onSurfaceVariant
        ReadState.READING -> MaterialTheme.colorScheme.onTertiaryContainer
        ReadState.READ -> MaterialTheme.colorScheme.onTertiary
    }

/** Closest `material-icons-core` equivalent for each design Material Symbol. */
fun OrderStatus.icon(): ImageVector =
    when (this) {
        OrderStatus.ORDERED -> TsundokuIcons.ShoppingCart
        OrderStatus.SHIPPED -> TsundokuIcons.Send
        OrderStatus.DELAYED -> TsundokuIcons.Warning
        OrderStatus.RECEIVED -> TsundokuIcons.CheckCircle
        OrderStatus.CANCELLED -> TsundokuIcons.Close
    }

fun ReadState.icon(): ImageVector =
    when (this) {
        ReadState.WANT -> TsundokuIcons.FavoriteBorder
        ReadState.READING -> TsundokuIcons.PlayArrow
        ReadState.READ -> TsundokuIcons.CheckCircle
    }
