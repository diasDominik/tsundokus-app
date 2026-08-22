package uk.tsundokus.features.orders.presentation.orderslist
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.koin.compose.viewmodel.koinViewModel
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewScreenSizes
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.components.NextArrivalHero
import uk.tsundokus.features.orders.presentation.components.OrderRow
import uk.tsundokus.features.orders.presentation.components.SectionHeader
import uk.tsundokus.features.orders.presentation.components.nowEpochMillis
import uk.tsundokus.features.orders.presentation.components.todayIso
import uk.tsundokus.features.orders.presentation.orderdetail.OrderDetailRoot

@Composable
fun OrdersListRoot(
    onOpenOrder: (String) -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    viewModel: OrdersListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is OrdersListEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message.asStringAsync())
        }
    }

    OrdersListScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenOrder = onOpenOrder,
        onEditOrder = onEditOrder,
        onReportDelay = onReportDelay,
        snackbarHostState = snackbarHostState,
    )
}

@Composable
private fun OrdersListScreen(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    onOpenOrder: (String) -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_MEDIUM_LOWER_BOUND,
        )
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            Column(
                modifier =
                    Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .fillMaxHeight(),
            ) {
                ListHeader(state = state, onAction = onAction)
                OrdersListBody(
                    state = state,
                    onOrderClick = { id -> onAction(OrdersListAction.OnOrderSelected(id)) },
                )
            }
            VerticalDivider(
                modifier = Modifier.fillMaxHeight(),
                color = MaterialTheme.colorScheme.outlineVariant,
            )
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val selectedId = state.selectedOrderId
                if (selectedId == null) {
                    EmptyState(
                        title = "Select an order",
                        caption = "Pick an order on the left to see its details.",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    key(selectedId) {
                        OrderDetailRoot(
                            orderId = selectedId,
                            snackbarHostState = snackbarHostState,
                            onEdit = { onEditOrder(selectedId) },
                            onReportDelay = { onReportDelay(selectedId) },
                            onBack = {},
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            ListHeader(state = state, onAction = onAction)
            OrdersListBody(
                state = state,
                onOrderClick = onOpenOrder,
            )
        }
    }
}

@Composable
private fun ListHeader(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Orders",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onAction(OrdersListAction.OnToggleSort) }) {
                Text("Sort · ${state.sort.label}")
            }
        }
        SyncStatusLine(state = state)
        VerticalSpacer(8.dp)
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(OrdersListAction.OnSearchQueryChange(it)) },
            placeholder = { Text("Search title, author, publisher") },
            leadingIcon = { Icon(TsundokuIcons.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        VerticalSpacer(8.dp)
        FilterChipsRow(state = state, onAction = onAction)
    }
}

@Composable
private fun SyncStatusLine(state: OrdersListState) {
    // Pending writes take priority (the actionable state); otherwise show freshness. Nothing to
    // show before the first sync.
    val (color, label) =
        when {
            state.pendingSyncCount > 0 -> {
                MaterialTheme.colorScheme.tertiary to
                    "${state.pendingSyncCount} unsynced ${if (state.pendingSyncCount == 1) "change" else "changes"}"
            }

            state.lastSyncedAt != null -> {
                MaterialTheme.colorScheme.onSurfaceVariant to "Synced ${relativeTime(state.lastSyncedAt)}"
            }

            else -> {
                return
            }
        }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Coarse relative time for the sync indicator; recomputed on each state emission. */
private fun relativeTime(millis: Long): String {
    val diff = nowEpochMillis() - millis
    return when {
        diff < 60_000L -> "just now"
        diff < 3_600_000L -> "${diff / 60_000L}m ago"
        diff < 86_400_000L -> "${diff / 3_600_000L}h ago"
        else -> "${diff / 86_400_000L}d ago"
    }
}

@Composable
private fun FilterChipsRow(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterChip(
            selected = state.statusFilter == null,
            onClick = { onAction(OrdersListAction.OnStatusFilterSelected(null)) },
            label = { Text("All ${state.counts[null] ?: 0}") },
            shape = RoundedCornerShape(20.dp),
        )
        OrderStatus.entries.forEach { status ->
            FilterChip(
                selected = state.statusFilter == status,
                onClick = { onAction(OrdersListAction.OnStatusFilterSelected(status)) },
                label = { Text("${status.label} ${state.counts[status] ?: 0}") },
                shape = RoundedCornerShape(20.dp),
            )
        }
    }
}

@Composable
private fun OrdersListBody(
    state: OrdersListState,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.displayed.isEmpty()) {
        EmptyState(
            title = "No orders here",
            caption = "Tap + to log your first order.",
            modifier = modifier.fillMaxSize(),
        )
        return
    }
    val today = remember { todayIso() }
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.nextArrival?.let { hero ->
            item(key = "hero") {
                NextArrivalHero(
                    order = hero,
                    today = today,
                    onClick = { onOrderClick(hero.id) },
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
        }
        state.grouped.forEach { (status, orders) ->
            item(key = "header_${status.name}") {
                SectionHeader(label = status.label, count = orders.size)
            }
            items(items = orders, key = { it.id }) { order ->
                OrderRow(
                    order = order,
                    today = today,
                    onClick = { onOrderClick(order.id) },
                    selected = order.id == state.selectedOrderId,
                )
            }
        }
    }
}

@Composable
private fun EmptyState(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        VerticalSpacer(8.dp)
        Text(
            text = caption,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@PreviewThemes
@PreviewScreenSizes
@Composable
private fun OrdersListScreenPreview() {
    TsundokuTheme {
        Surface {
            OrdersListScreen(
                state = previewState(),
                onAction = {},
                onOpenOrder = {},
                onEditOrder = {},
                onReportDelay = {},
                snackbarHostState = remember { SnackbarHostState() },
            )
        }
    }
}

private fun previewState(): OrdersListState {
    val orders =
        listOf(
            Order(
                id = "1",
                title = "Chainsaw Man",
                author = "Tatsuki Fujimoto",
                volume = "Vol. 12",
                store = "Amazon",
                price = 12.99,
                status = OrderStatus.SHIPPED,
                eta = "2026-07-02",
                createdAt = 5,
            ),
            Order(
                id = "2",
                title = "Berserk",
                author = "Kentaro Miura",
                volume = "Vol. 41",
                store = "Crunchyroll",
                price = 49.99,
                status = OrderStatus.DELAYED,
                delayedTo = "2026-08-15",
                createdAt = 4,
            ),
            Order(
                id = "3",
                title = "Vinland Saga",
                author = "Makoto Yukimura",
                volume = "Vol. 1",
                store = "Kinokuniya",
                price = 19.99,
                status = OrderStatus.ORDERED,
                releaseDate = "2026-09-01",
                createdAt = 3,
            ),
        )
    return OrdersListState(
        isLoading = false,
        allOrders = orders,
        displayed = orders,
        nextArrival = orders.first(),
        grouped =
            mapOf(
                OrderStatus.DELAYED to orders.filter { it.status == OrderStatus.DELAYED },
                OrderStatus.SHIPPED to orders.filter { it.status == OrderStatus.SHIPPED },
                OrderStatus.ORDERED to orders.filter { it.status == OrderStatus.ORDERED },
            ),
        selectedOrderId = "1",
        counts = mapOf(null to 3, OrderStatus.SHIPPED to 1, OrderStatus.DELAYED to 1, OrderStatus.ORDERED to 1),
        pendingSyncCount = 2,
    )
}
