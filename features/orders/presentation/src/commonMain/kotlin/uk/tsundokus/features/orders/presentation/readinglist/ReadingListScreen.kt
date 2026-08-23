package uk.tsundokus.features.orders.presentation.readinglist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.components.SectionHeader
import uk.tsundokus.features.orders.presentation.components.StatusTile
import uk.tsundokus.features.orders.presentation.components.containerColor
import uk.tsundokus.features.orders.presentation.components.fullLabelRes
import uk.tsundokus.features.orders.presentation.components.labelRes
import uk.tsundokus.features.orders.presentation.components.onContainerColor

@Composable
fun ReadingListRoot(
    onOpenOrder: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: ReadingListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReadingListEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    ReadingListScreen(
        state = state,
        onCycleReadState = viewModel::onCycleReadState,
        onOpenOrder = onOpenOrder,
    )
}

@Composable
private fun ReadingListScreen(
    state: ReadingListState,
    onCycleReadState: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = "Reading",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (state.grouped.isEmpty()) {
            EmptyShelf(modifier = Modifier.fillMaxSize())
            return@Column
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.grouped.forEach { (readState, orders) ->
                item(key = "header_${readState.name}") {
                    SectionHeader(
                        label = stringResource(readState.fullLabelRes),
                        count = orders.size,
                        dotColor = readState.dotColor(),
                    )
                }
                items(items = orders, key = { it.id }) { order ->
                    ReadingRow(
                        order = order,
                        onClick = { onOpenOrder(order.id) },
                        onReadStateClick = { onCycleReadState(order.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingRow(
    order: Order,
    onClick: () -> Unit,
    onReadStateClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.padding(vertical = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatusTile(status = order.status)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (order.subtitle.isNotBlank()) {
                    Text(
                        text = order.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            HorizontalSpacer(8.dp)
            ReadStateChip(readState = order.readState, onClick = onReadStateClick)
        }
    }
}

@Composable
private fun ReadStateChip(
    readState: ReadState,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = readState.containerColor(),
        contentColor = readState.onContainerColor(),
    ) {
        Text(
            text = stringResource(readState.labelRes),
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun EmptyShelf(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Nothing on your shelf yet.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
@ReadOnlyComposable
private fun ReadState.dotColor(): Color =
    when (this) {
        ReadState.WANT -> MaterialTheme.colorScheme.onSurfaceVariant
        ReadState.READING -> MaterialTheme.colorScheme.primary
        ReadState.READ -> MaterialTheme.colorScheme.tertiary
    }

@PreviewThemes
@Composable
private fun ReadingListScreenPreview() {
    TsundokuTheme {
        Surface {
            ReadingListScreen(
                state =
                    ReadingListState(
                        isLoading = false,
                        grouped =
                            mapOf(
                                ReadState.READING to
                                    listOf(
                                        Order(
                                            id = "1",
                                            title = "Vinland Saga",
                                            author = "Makoto Yukimura",
                                            volume = "Vol. 3",
                                            status = OrderStatus.RECEIVED,
                                            readState = ReadState.READING,
                                        ),
                                    ),
                                ReadState.WANT to
                                    listOf(
                                        Order(
                                            id = "2",
                                            title = "Berserk",
                                            author = "Kentaro Miura",
                                            volume = "Vol. 41",
                                            status = OrderStatus.ORDERED,
                                            readState = ReadState.WANT,
                                        ),
                                    ),
                            ),
                    ),
                onCycleReadState = {},
                onOpenOrder = {},
            )
        }
    }
}
