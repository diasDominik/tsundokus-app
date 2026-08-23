package uk.tsundokus.features.orders.presentation.orderslist
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_MEDIUM_LOWER_BOUND
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_clear_filters
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_clear_search_cd
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_detail_placeholder_caption
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_detail_placeholder_title
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_empty_caption
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_empty_title
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_filter_all
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_no_matches_filter_caption
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_no_matches_search_caption
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_no_matches_title
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_refresh_cd
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_search_placeholder
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_sort
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_status_filter
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_synced
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_time_days
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_time_hours
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_time_just_now
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_time_minutes
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_title
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_unsynced_changes
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewScreenSizes
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.isCommandOrControlPressed
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.SortDirection
import uk.tsundokus.features.orders.presentation.components.NextArrivalHero
import uk.tsundokus.features.orders.presentation.components.OrderRow
import uk.tsundokus.features.orders.presentation.components.SectionHeader
import uk.tsundokus.features.orders.presentation.components.labelRes
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
    val searchFocusRequester = remember { FocusRequester() }
    // Ctrl/Cmd+F jumps to search. Handled as a *preview* event so it works even while a text field
    // holds focus, and only when the modifier is down so ordinary typing is untouched.
    val shortcuts =
        Modifier.onPreviewKeyEvent { event ->
            if (event.type == KeyEventType.KeyDown && event.isCommandOrControlPressed && event.key == Key.F) {
                searchFocusRequester.requestFocus()
                true
            } else {
                false
            }
        }
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize().then(shortcuts)) {
            Column(
                modifier =
                    Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .fillMaxHeight(),
            ) {
                ListHeader(state = state, onAction = onAction, searchFocusRequester = searchFocusRequester)
                OrdersListBody(
                    state = state,
                    onAction = onAction,
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
                        title = stringResource(Res.string.orders_list_detail_placeholder_title),
                        caption = stringResource(Res.string.orders_list_detail_placeholder_caption),
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
        Column(modifier = modifier.fillMaxSize().then(shortcuts)) {
            ListHeader(state = state, onAction = onAction, searchFocusRequester = searchFocusRequester)
            OrdersListBody(
                state = state,
                onAction = onAction,
                onOrderClick = onOpenOrder,
            )
        }
    }
}

@Composable
private fun ListHeader(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.orders_list_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            SortMenu(state = state, onAction = onAction)
        }
        SyncStatusLine(state = state, onAction = onAction)
        VerticalSpacer(8.dp)
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(OrdersListAction.OnSearchQueryChange(it)) },
            placeholder = { Text(stringResource(Res.string.orders_list_search_placeholder)) },
            leadingIcon = { Icon(TsundokuIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(OrdersListAction.OnSearchQueryChange("")) }) {
                        Icon(
                            TsundokuIcons.Close,
                            contentDescription = stringResource(Res.string.orders_list_clear_search_cd),
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth().focusRequester(searchFocusRequester),
        )
        VerticalSpacer(8.dp)
        FilterChipsRow(state = state, onAction = onAction)
    }
}

/**
 * The sort control. Re-picking the active sort reverses it, which the arrow in the trailing
 * position reflects — the previous cycle-through-four-modes button gave no way to see the options
 * or to reverse any of them.
 */
@Composable
private fun SortMenu(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(Res.string.orders_list_sort, stringResource(state.sort.labelRes)))
            Icon(
                imageVector =
                    if (state.sortDirection == SortDirection.ASCENDING) {
                        TsundokuIcons.ArrowUpward
                    } else {
                        TsundokuIcons.ArrowDownward
                    },
                contentDescription = stringResource(state.sortDirection.labelRes),
                modifier = Modifier.size(16.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            OrderSort.entries.forEach { sort ->
                val isSelected = sort == state.sort
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    onClick = {
                        onAction(OrdersListAction.OnSortSelected(sort))
                        // Kept open on the active entry: reversing is a repeat tap, and closing the
                        // menu on every flip would make that a two-click round trip each time.
                        if (!isSelected) expanded = false
                    },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(TsundokuIcons.Check, contentDescription = null)
                        }
                    },
                    trailingIcon = {
                        if (isSelected) {
                            Icon(
                                imageVector =
                                    if (state.sortDirection == SortDirection.ASCENDING) {
                                        TsundokuIcons.ArrowUpward
                                    } else {
                                        TsundokuIcons.ArrowDownward
                                    },
                                contentDescription = stringResource(state.sortDirection.labelRes),
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Sync freshness, and the only sync affordance that exists on every platform: pull-to-refresh is a
 * touch gesture, so desktop and web need something clickable.
 */
@Composable
private fun SyncStatusLine(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
) {
    // Pending writes take priority (the actionable state); otherwise show freshness. Nothing to
    // show before the first sync.
    val (color, label) =
        when {
            state.pendingSyncCount > 0 -> {
                MaterialTheme.colorScheme.tertiary to
                    pluralStringResource(
                        Res.plurals.orders_list_unsynced_changes,
                        state.pendingSyncCount,
                        state.pendingSyncCount,
                    )
            }

            state.lastSyncedAt != null -> {
                MaterialTheme.colorScheme.onSurfaceVariant to
                    stringResource(Res.string.orders_list_synced, relativeTime(state.lastSyncedAt))
            }

            else -> {
                return
            }
        }
    val refreshLabel = stringResource(Res.string.orders_list_refresh_cd)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier =
            Modifier
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = !state.isRefreshing, onClickLabel = refreshLabel) {
                    onAction(OrdersListAction.OnRefresh)
                }.padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
    }
}

/** Coarse relative time for the sync indicator; recomputed on each state emission. */
@Composable
private fun relativeTime(millis: Long): String {
    val diff = nowEpochMillis() - millis
    return when {
        diff < 60_000L -> stringResource(Res.string.orders_list_time_just_now)
        diff < 3_600_000L -> stringResource(Res.string.orders_list_time_minutes, diff / 60_000L)
        diff < 86_400_000L -> stringResource(Res.string.orders_list_time_hours, diff / 3_600_000L)
        else -> stringResource(Res.string.orders_list_time_days, diff / 86_400_000L)
    }
}

@Composable
private fun FilterChipsRow(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
) {
    val scrollState = rememberScrollState()
    // The row scrolls but gave no sign of it: chips simply ran off the edge, and the last two were
    // invisible in a narrow pane. A fade on whichever side has more chips shows there is more to
    // reach, and clearing the filters returns the row to the start so it never reads as truncated.
    val fadeColor = MaterialTheme.colorScheme.background
    LaunchedEffect(state.isFiltered) {
        if (!state.isFiltered) scrollState.animateScrollTo(0)
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilterChip(
                selected = state.statusFilter == null,
                onClick = { onAction(OrdersListAction.OnStatusFilterSelected(null)) },
                label = { Text(stringResource(Res.string.orders_list_filter_all, state.counts[null] ?: 0)) },
                shape = RoundedCornerShape(20.dp),
            )
            OrderStatus.entries.forEach { status ->
                FilterChip(
                    selected = state.statusFilter == status,
                    onClick = { onAction(OrdersListAction.OnStatusFilterSelected(status)) },
                    label = {
                        Text(
                            stringResource(
                                Res.string.orders_list_status_filter,
                                stringResource(status.labelRes),
                                state.counts[status] ?: 0,
                            ),
                        )
                    },
                    shape = RoundedCornerShape(20.dp),
                )
            }
        }
        // matchParentSize, not fillMaxHeight: the fades must take their height *from* the chip row
        // without contributing to layout. Filling height made this Box claim the whole column and
        // left the list itself zero pixels tall.
        Box(modifier = Modifier.matchParentSize()) {
            EdgeFade(Alignment.CenterStart, fadeColor, visible = scrollState.value > 0)
            EdgeFade(
                Alignment.CenterEnd,
                fadeColor,
                visible = scrollState.value < scrollState.maxValue,
            )
        }
    }
}

/** A short gradient over the scrolling edge, purely a hint that the row continues. */
@Composable
private fun BoxScope.EdgeFade(
    alignment: Alignment,
    color: Color,
    visible: Boolean,
) {
    if (!visible) return
    val colors =
        if (alignment == Alignment.CenterStart) {
            listOf(color, Color.Transparent)
        } else {
            listOf(Color.Transparent, color)
        }
    Box(
        modifier =
            Modifier
                .align(alignment)
                .width(24.dp)
                .fillMaxHeight()
                .background(Brush.horizontalGradient(colors)),
    )
}

@Composable
private fun OrdersListBody(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state.displayed.isEmpty()) {
        if (state.isFiltered) {
            NoMatchesState(state = state, onAction = onAction, modifier = modifier.fillMaxSize())
        } else {
            EmptyState(
                title = stringResource(Res.string.orders_list_empty_title),
                caption = stringResource(Res.string.orders_list_empty_caption),
                modifier = modifier.fillMaxSize(),
            )
        }
        return
    }
    val today = remember { todayIso() }
    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = { onAction(OrdersListAction.OnRefresh) },
        modifier = modifier.fillMaxSize(),
    ) {
        OrdersLazyColumn(state = state, today = today, onOrderClick = onOrderClick)
    }
}

/**
 * Arrow keys walk the list and Enter opens the highlighted row — but only while the list itself
 * holds focus, so arrows still move the caret when the user is in the search field.
 */
@Composable
private fun Modifier.listKeyboardNavigation(
    state: OrdersListState,
    onOrderClick: (String) -> Unit,
): Modifier {
    val focusRequester = remember { FocusRequester() }
    return this
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
            val displayed = state.displayed
            if (displayed.isEmpty()) return@onPreviewKeyEvent false
            val index = displayed.indexOfFirst { it.id == state.selectedOrderId }
            when (event.key) {
                Key.DirectionDown -> {
                    onOrderClick(displayed[(index + 1).coerceAtMost(displayed.lastIndex)].id)
                    true
                }

                Key.DirectionUp -> {
                    onOrderClick(displayed[(index - 1).coerceAtLeast(0)].id)
                    true
                }

                Key.Enter, Key.NumPadEnter -> {
                    displayed.getOrNull(index)?.let { onOrderClick(it.id) } != null
                }

                else -> {
                    false
                }
            }
        }
}

@Composable
private fun OrdersLazyColumn(
    state: OrdersListState,
    today: String,
    onOrderClick: (String) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().listKeyboardNavigation(state, onOrderClick),
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
                SectionHeader(label = stringResource(status.labelRes), count = orders.size)
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

/**
 * Shown when the user's own search or status filter hides everything — a different situation from
 * having no orders at all, and one they can undo right here rather than hunting for what to reset.
 */
@Composable
private fun NoMatchesState(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    EmptyState(
        title = stringResource(Res.string.orders_list_no_matches_title),
        caption =
            if (state.searchQuery.isNotBlank()) {
                stringResource(Res.string.orders_list_no_matches_search_caption, state.searchQuery)
            } else {
                stringResource(Res.string.orders_list_no_matches_filter_caption)
            },
        modifier = modifier,
    ) {
        TsundokuButton(
            text = stringResource(Res.string.orders_list_clear_filters),
            onClick = {
                onAction(OrdersListAction.OnSearchQueryChange(""))
                onAction(OrdersListAction.OnStatusFilterSelected(null))
            },
            style = TsundokuButtonStyle.Secondary,
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    caption: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null,
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
        action?.let {
            VerticalSpacer(16.dp)
            it()
        }
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
