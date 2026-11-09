package uk.tsundokus.features.orders.presentation.orderdetail

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.navigation3.runtime.NavKey
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_delete
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_edit
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_estimate_all_stores
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_estimate_default
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_estimate_store
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_delayed_to
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_eta
import tsundokuapp.features.orders.presentation.generated.resources.order_detail_fact_isbn
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
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.navigation.TopBarActions
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.stats.ArrivalEstimate
import uk.tsundokus.features.orders.domain.stats.DEFAULT_SHIPPING_DAYS
import uk.tsundokus.features.orders.domain.stats.EstimateBasis
import uk.tsundokus.features.orders.presentation.components.BookCover
import uk.tsundokus.features.orders.presentation.components.ESTIMATE_MARK
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
    /** This screen's own route when it has one; null inside the orders list's detail pane. */
    navKey: NavKey?,
    snackbar: SnackbarController,
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
            is OrderDetailEvent.ShowMessage -> snackbar.show(event.message)
            OrderDetailEvent.Deleted -> onBack()
        }
    }

    var confirmingDelete by remember { mutableStateOf(false) }
    val order = state.order
    // A routed detail screen puts Edit and Delete in its own top bar. In the list's detail pane
    // there is no bar of its own, so the same buttons sit at the top of the content instead.
    if (navKey != null && order != null) {
        TopBarActions(navKey) {
            OrderManageActions(onEdit = onEdit, onDelete = { confirmingDelete = true })
        }
    }

    OrderDetailScreen(
        state = state,
        onPrimaryAction = viewModel::onPrimaryAction,
        onSetReadState = viewModel::onSetReadState,
        onEdit = onEdit,
        onReportDelay = onReportDelay,
        onDelete = { confirmingDelete = true },
        showManageActions = navKey == null,
        modifier = modifier,
    )

    if (confirmingDelete && order != null) {
        OrderDeleteConfirmDialog(
            orderTitle = order.title,
            onConfirm = {
                confirmingDelete = false
                viewModel.onDelete()
            },
            onDismiss = { confirmingDelete = false },
        )
    }
}

/** Editing and deleting: occasional, so icons in the top bar rather than full-width buttons. */
@Composable
private fun OrderManageActions(
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    IconButton(onClick = onEdit) {
        Icon(TsundokuIcons.Edit, contentDescription = stringResource(Res.string.order_detail_edit))
    }
    IconButton(onClick = onDelete) {
        Icon(TsundokuIcons.Delete, contentDescription = stringResource(Res.string.order_detail_delete))
    }
}

@Composable
private fun OrderDetailScreen(
    state: OrderDetailState,
    onPrimaryAction: () -> Unit,
    onSetReadState: (ReadState) -> Unit,
    onEdit: () -> Unit,
    onReportDelay: () -> Unit,
    onDelete: () -> Unit,
    showManageActions: Boolean,
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
                showManageActions = showManageActions,
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
    showManageActions: Boolean,
) {
    if (showManageActions) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            OrderManageActions(onEdit = onEdit, onDelete = onDelete)
        }
    }
    Row(verticalAlignment = Alignment.Top) {
        if (order.hasCover && order.isbn.isNotBlank()) {
            BookCover(isbn = order.isbn, width = 88.dp, modifier = Modifier.padding(end = 16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
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
        }
    }

    VerticalSpacer(20.dp)
    SectionLabel(stringResource(Res.string.order_detail_section_reading))
    VerticalSpacer(8.dp)
    ReadStateSegmented(selected = order.readState, onSelect = onSetReadState)

    VerticalSpacer(20.dp)
    SectionLabel(stringResource(Res.string.order_detail_section_timeline))
    VerticalSpacer(8.dp)
    StatusTimeline(nodes = state.timeline)

    if (order.hasFactsBeyondTimeline()) {
        VerticalSpacer(20.dp)
        SectionLabel(stringResource(Res.string.order_detail_section_details))
        VerticalSpacer(4.dp)
        OrderFacts(order = order, estimate = state.estimate)
    }
    val canReportDelay = order.status != OrderStatus.RECEIVED && order.status != OrderStatus.CANCELLED
    if (state.primaryAction != null || canReportDelay) {
        VerticalSpacer(20.dp)
        state.primaryAction?.let { action ->
            TsundokuButton(
                text = stringResource(action.labelRes),
                onClick = onPrimaryAction,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (canReportDelay) {
            if (state.primaryAction != null) VerticalSpacer(8.dp)
            TsundokuButton(
                text = stringResource(Res.string.order_detail_report_delay),
                onClick = onReportDelay,
                style = TsundokuButtonStyle.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    VerticalSpacer(16.dp)
}

/*
 * The fact sheet lists only what the header and the timeline do not already show: the price is in
 * the header, the order date is always on the timeline, and each status's timeline carries its own
 * dates (see buildTimeline). Showing them again made the screen read every date twice.
 */
private val Order.showsShipDate get() = shipDate.isNotBlank() && status !in SHIPPED_OR_RECEIVED
private val Order.showsRelease get() = releaseDate.isNotBlank() && status != OrderStatus.ORDERED
private val Order.showsEta get() = status !in SHIPPED_OR_RECEIVED
private val Order.showsDelayedTo get() = delayedTo.isNotBlank() && status != OrderStatus.DELAYED
private val Order.showsReceived get() = receivedDate.isNotBlank() && status != OrderStatus.RECEIVED
private val SHIPPED_OR_RECEIVED = setOf(OrderStatus.SHIPPED, OrderStatus.RECEIVED)

private fun Order.hasFactsBeyondTimeline(): Boolean =
    store.isNotBlank() || isbn.isNotBlank() || showsRelease || showsShipDate ||
        (showsEta && eta.isNotBlank()) || showsDelayedTo || showsReceived

@Composable
private fun OrderFacts(
    order: Order,
    estimate: ArrivalEstimate?,
) {
    Column {
        if (order.store.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_store), order.store)
        }
        if (order.isbn.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_isbn), order.isbn)
        }
        if (order.showsRelease) {
            FactRow(stringResource(Res.string.order_detail_fact_release), fmtDate(order.releaseDate))
        }
        if (order.showsShipDate) {
            FactRow(stringResource(Res.string.order_detail_fact_shipped), fmtDate(order.shipDate))
        }
        if (order.showsEta && order.eta.isNotBlank()) {
            FactRow(stringResource(Res.string.order_detail_fact_eta), fmtDate(order.eta))
        } else if (order.showsEta && estimate != null) {
            FactRow(
                label = stringResource(Res.string.order_detail_fact_eta),
                value = ESTIMATE_MARK + fmtDate(estimate.date),
                caption = estimateCaption(estimate),
            )
        }
        if (order.showsDelayedTo) {
            FactRow(stringResource(Res.string.order_detail_fact_delayed_to), fmtDate(order.delayedTo))
        }
        if (order.showsReceived) {
            FactRow(stringResource(Res.string.order_detail_fact_received), fmtDate(order.receivedDate))
        }
    }
}

/** Where an estimate comes from, so it reads as one: "Based on 9 deliveries from this store". */
@Composable
private fun estimateCaption(estimate: ArrivalEstimate): String =
    when (estimate.basis) {
        EstimateBasis.STORE -> {
            pluralStringResource(Res.plurals.order_detail_estimate_store, estimate.sample, estimate.sample)
        }

        EstimateBasis.ALL_STORES -> {
            pluralStringResource(Res.plurals.order_detail_estimate_all_stores, estimate.sample, estimate.sample)
        }

        EstimateBasis.DEFAULT -> {
            pluralStringResource(
                Res.plurals.order_detail_estimate_default,
                DEFAULT_SHIPPING_DAYS,
                DEFAULT_SHIPPING_DAYS,
            )
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
                showManageActions = false,
            )
        }
    }
}
