package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.components.FactRow
import uk.tsundokus.features.orders.presentation.components.ReadStateSegmented
import uk.tsundokus.features.orders.presentation.components.StatusChip
import uk.tsundokus.features.orders.presentation.components.StatusTimeline
import uk.tsundokus.features.orders.presentation.components.TimelineNode
import uk.tsundokus.features.orders.presentation.components.TimelineNodeState
import uk.tsundokus.features.orders.presentation.components.fmtDate
import uk.tsundokus.features.orders.presentation.components.priceLabel

@Composable
fun OrderDetailRoot(
    orderId: String,
    snackbarHostState: SnackbarHostState,
    onEdit: () -> Unit,
    onReportDelay: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderDetailViewModel =
        koinViewModel(
            key = orderId,
            parameters = { parametersOf(orderId) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is OrderDetailEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.asStringAsync())
            OrderDetailEvent.Deleted -> onBack()
        }
    }

    OrderDetailScreen(
        state = state,
        onPrimaryAction = viewModel::onPrimaryAction,
        onSetReadState = viewModel::onSetReadState,
        onEdit = onEdit,
        onReportDelay = onReportDelay,
        onDelete = viewModel::onDelete,
        modifier = modifier,
    )
}

@Composable
private fun OrderDetailScreen(
    state: OrderDetailState,
    onPrimaryAction: () -> Unit,
    onSetReadState: (ReadState) -> Unit,
    onEdit: () -> Unit,
    onReportDelay: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = state.order
    if (order == null) {
        Box(
            modifier = modifier.fillMaxSize().padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Order not found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
    ) {
        Text(
            text = listOf(order.title, order.volume).filter { it.isNotBlank() }.joinToString(" "),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        if (order.byline.isNotBlank()) {
            Text(
                text = order.byline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        VerticalSpacer(12.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusChip(status = order.status)
            Text(
                text = priceLabel(order),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f).padding(start = 12.dp),
            )
        }

        VerticalSpacer(20.dp)
        SectionLabel("Reading")
        VerticalSpacer(8.dp)
        ReadStateSegmented(selected = order.readState, onSelect = onSetReadState)

        VerticalSpacer(20.dp)
        SectionLabel("Timeline")
        VerticalSpacer(8.dp)
        StatusTimeline(nodes = state.timeline)

        VerticalSpacer(20.dp)
        SectionLabel("Details")
        VerticalSpacer(4.dp)
        OrderFacts(order = order)

        VerticalSpacer(20.dp)
        state.primaryAction?.let { action ->
            TsundokuButton(
                text = action.label,
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            )
            VerticalSpacer(8.dp)
        }
        TsundokuButton(
            text = "Edit",
            onClick = onEdit,
            style = TsundokuButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        if (order.status != OrderStatus.RECEIVED && order.status != OrderStatus.CANCELLED) {
            VerticalSpacer(8.dp)
            TsundokuButton(
                text = "Report delay",
                onClick = onReportDelay,
                style = TsundokuButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        VerticalSpacer(8.dp)
        TsundokuButton(
            text = "Delete order",
            onClick = onDelete,
            style = TsundokuButtonStyle.DestructiveSecondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun OrderFacts(order: Order) {
    Column {
        if (order.store.isNotBlank()) {
            FactRow("Store", order.store)
        }
        FactRow("Price", priceLabel(order))
        if (order.orderDate.isNotBlank()) {
            FactRow("Ordered", fmtDate(order.orderDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (order.releaseDate.isNotBlank()) {
            FactRow("Release", fmtDate(order.releaseDate))
        }
        if (order.shipDate.isNotBlank()) {
            FactRow("Shipped", fmtDate(order.shipDate))
        }
        if (order.eta.isNotBlank()) {
            FactRow("ETA", fmtDate(order.eta))
        }
        if (order.delayedTo.isNotBlank()) {
            FactRow("Delayed to", fmtDate(order.delayedTo))
        }
        if (order.receivedDate.isNotBlank()) {
            FactRow("Received", fmtDate(order.receivedDate))
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

@PreviewThemes
@Composable
private fun OrderDetailScreenPreview() {
    TsundokuTheme {
        Surface {
            OrderDetailScreen(
                state =
                    OrderDetailState(
                        isLoading = false,
                        order =
                            Order(
                                id = "1",
                                title = "Chainsaw Man",
                                author = "Tatsuki Fujimoto",
                                publisher = "Viz Media",
                                volume = "Vol. 12",
                                store = "Amazon",
                                price = 12.99,
                                status = OrderStatus.SHIPPED,
                                orderDate = "2026-06-10",
                                shipDate = "2026-06-20",
                                eta = "2026-07-02",
                            ),
                        timeline =
                            listOf(
                                TimelineNode("Ordered", "10 Jun 2026", TimelineNodeState.DONE),
                                TimelineNode("Shipped", "20 Jun 2026", TimelineNodeState.DONE),
                                TimelineNode("Arriving", "2 Jul 2026", TimelineNodeState.CURRENT),
                            ),
                        primaryAction = PrimaryAction.MARK_RECEIVED,
                    ),
                onPrimaryAction = {},
                onSetReadState = {},
                onEdit = {},
                onReportDelay = {},
                onDelete = {},
            )
        }
    }
}
