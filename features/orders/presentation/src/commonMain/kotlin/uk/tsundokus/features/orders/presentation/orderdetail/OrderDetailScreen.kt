package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_delete
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_edit
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_delayed_to
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_eta
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_ordered
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_price
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_received
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_release
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_shipped
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_store
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_not_found
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_report_delay
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_section_details
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_section_reading
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_section_timeline
import tsundokuapp.features.orders.presentation.generated.resources.timeline_arriving
import tsundokuapp.features.orders.presentation.generated.resources.timeline_ordered
import tsundokuapp.features.orders.presentation.generated.resources.timeline_shipped
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
import uk.tsundokus.features.orders.presentation.components.OrderDeleteConfirmDialog
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
                text = stringResource(Res.string.order_detail_not_found),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    // Capped and centred: in the expanded list-detail layout this pane takes whatever width is
    // left over, and unbounded fact rows put the label and its value at opposite ends of a very
    // wide screen.
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = DETAIL_MAX_WIDTH).fillMaxWidth().padding(16.dp)) {
            OrderDetailContent(
                state = state,
                order = order,
                onPrimaryAction = onPrimaryAction,
                onSetReadState = onSetReadState,
                onEdit = onEdit,
                onReportDelay = onReportDelay,
                onDelete = onDelete,
            )
        }
    }
}

private val DETAIL_MAX_WIDTH = 600.dp

@Composable
private fun ColumnScope.OrderDetailContent(
    state: OrderDetailState,
    order: Order,
    onPrimaryAction: () -> Unit,
    onSetReadState: (ReadState) -> Unit,
    onEdit: () -> Unit,
    onReportDelay: () -> Unit,
    onDelete: () -> Unit,
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
    SectionLabel(stringResource(Res.string.order_detail_section_reading))
    VerticalSpacer(8.dp)
    ReadStateSegmented(selected = order.readState, onSelect = onSetReadState)

    VerticalSpacer(20.dp)
    SectionLabel(stringResource(Res.string.order_detail_section_timeline))
    VerticalSpacer(8.dp)
    StatusTimeline(nodes = state.timeline)

    VerticalSpacer(20.dp)
    SectionLabel(stringResource(Res.string.order_detail_section_details))
    VerticalSpacer(4.dp)
    OrderFacts(order = order)

    VerticalSpacer(20.dp)
    state.primaryAction?.let { action ->
        TsundokuButton(
            text = stringResource(action.labelRes),
            onClick = onPrimaryAction,
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
    }
    TsundokuButton(
        text = stringResource(Res.string.order_detail_edit),
        onClick = onEdit,
        style = TsundokuButtonStyle.Secondary,
        modifier = Modifier.fillMaxWidth(),
    )
    if (order.status != OrderStatus.RECEIVED && order.status != OrderStatus.CANCELLED) {
        VerticalSpacer(8.dp)
        TsundokuButton(
            text = stringResource(Res.string.order_detail_report_delay),
            onClick = onReportDelay,
            style = TsundokuButtonStyle.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
    }
    VerticalSpacer(8.dp)
    var confirmingDelete by remember { mutableStateOf(false) }
    TsundokuButton(
        text = stringResource(Res.string.order_detail_delete),
        onClick = { confirmingDelete = true },
        style = TsundokuButtonStyle.DestructiveSecondary,
        modifier = Modifier.fillMaxWidth(),
    )
    if (confirmingDelete) {
        OrderDeleteConfirmDialog(
            orderTitle = order.title,
            onConfirm = {
                confirmingDelete = false
                onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

@Composable
private fun OrderFacts(order: Order) {
    Column {
        if (order.store.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_store), order.store)
        }
        FactRow(stringResource(Res.string.order_detail_fact_price), priceLabel(order))
        if (order.orderDate.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_ordered), fmtDate(order.orderDate))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        if (order.releaseDate.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_release), fmtDate(order.releaseDate))
        }
        if (order.shipDate.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_shipped), fmtDate(order.shipDate))
        }
        if (order.eta.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_eta), fmtDate(order.eta))
        }
        if (order.delayedTo.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_delayed_to), fmtDate(order.delayedTo))
        }
        if (order.receivedDate.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_received), fmtDate(order.receivedDate))
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
                                TimelineNode(Res.string.timeline_ordered, "10 Jun 2026", TimelineNodeState.DONE),
                                TimelineNode(Res.string.timeline_shipped, "20 Jun 2026", TimelineNodeState.DONE),
                                TimelineNode(
                                    Res.string.timeline_arriving,
                                    "2 Jul 2026",
                                    TimelineNodeState.CURRENT,
                                ),
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
