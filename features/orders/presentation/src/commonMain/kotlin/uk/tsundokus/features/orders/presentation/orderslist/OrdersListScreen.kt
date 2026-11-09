package uk.tsundokus.features.orders.presentation.orderslist
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
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
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_scan_cd
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
import tsundokuapp.features.orders.presentation.generated.resources.selection_delete
import tsundokuapp.features.orders.presentation.generated.resources.selection_mark_cancelled
import tsundokuapp.features.orders.presentation.generated.resources.selection_mark_received
import tsundokuapp.features.orders.presentation.generated.resources.selection_mark_shipped
import tsundokuapp.features.orders.presentation.generated.resources.selection_more_cd
import tsundokuapp.features.orders.presentation.generated.resources.selection_undo
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.buttons.TsundokuButtonStyle
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewScreenSizes
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.navigation.HideFab
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.isCommandOrControlPressed
import uk.tsundokus.core.presentation.util.rememberSnackbarController
import uk.tsundokus.features.orders.domain.dates.nowEpochMillis
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderChange
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.SortDirection
import uk.tsundokus.features.orders.presentation.components.EmptyState
import uk.tsundokus.features.orders.presentation.components.FadingChipRow
import uk.tsundokus.features.orders.presentation.components.ListSkeleton
import uk.tsundokus.features.orders.presentation.components.OrderRow
import uk.tsundokus.features.orders.presentation.components.SectionHeader
import uk.tsundokus.features.orders.presentation.components.labelRes
import uk.tsundokus.features.orders.presentation.navigation.Orders
import uk.tsundokus.features.orders.presentation.orderdetail.OrderDetailRoot
import uk.tsundokus.features.orders.presentation.selection.DeleteSelectedConfirmDialog
import uk.tsundokus.features.orders.presentation.selection.ReadStateMenuButton
import uk.tsundokus.features.orders.presentation.selection.SelectionBackHandler
import uk.tsundokus.features.orders.presentation.selection.SelectionHeader

@Composable
fun OrdersListRoot(
    onOpenOrder: (String) -> Unit,
    onScanToReceive: () -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    snackbar: SnackbarController,
    viewModel: OrdersListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is OrdersListEvent.ShowMessage -> {
                snackbar.show(event.message)
            }

            is OrdersListEvent.ShowUndoableMessage -> {
                snackbar.show(event.message, UiText.Resource(Res.string.selection_undo)) {
                    viewModel.onAction(OrdersListAction.OnUndo)
                }
            }
        }
    }

    OrdersListScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenOrder = onOpenOrder,
        onScanToReceive = onScanToReceive,
        onEditOrder = onEditOrder,
        onReportDelay = onReportDelay,
        snackbar = snackbar,
    )
}

@Composable
private fun OrdersListScreen(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    onOpenOrder: (String) -> Unit,
    onScanToReceive: () -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    snackbar: SnackbarController,
    modifier: Modifier = Modifier,
) {
    // Two panes only once the window is wide enough for both to be worth reading. At medium width
    // — an unfolded Z Fold is 752dp — the navigation rail plus the list pane leave the detail
    // narrower than a phone screen, so the list gets the window to itself and an order opens on
    // its own screen instead.
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(
            WIDTH_DP_EXPANDED_LOWER_BOUND,
        )
    val searchFocusRequester = remember { FocusRequester() }
    // Ctrl/Cmd+F jumps to search. Handled as a *preview* event so it works even while a text field
    // holds focus, and only when the modifier is down so ordinary typing is untouched. Escape leaves
    // picking: the shell only acts on it where there is a screen to go back to.
    val shortcuts =
        Modifier.onPreviewKeyEvent { event ->
            when {
                event.type != KeyEventType.KeyDown -> {
                    false
                }

                event.isCommandOrControlPressed && event.key == Key.F -> {
                    searchFocusRequester.requestFocus()
                    true
                }

                event.key == Key.Escape && state.isPicking -> {
                    onAction(OrdersListAction.OnStopPicking)
                    true
                }

                else -> {
                    false
                }
            }
        }
    SelectionBackHandler(isSelecting = state.isPicking, onClear = { onAction(OrdersListAction.OnStopPicking) })
    if (state.isPicking) HideFab(Orders)
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize().then(shortcuts)) {
            Column(
                modifier =
                    Modifier
                        .widthIn(min = 320.dp, max = 400.dp)
                        .fillMaxHeight(),
            ) {
                ListHeader(
                    state = state,
                    onAction = onAction,
                    onScanToReceive = onScanToReceive,
                    searchFocusRequester = searchFocusRequester,
                )
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
                            navKey = null,
                            snackbar = snackbar,
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
        // Capped and centred rather than stretched: on a window this wide a row would otherwise
        // put its price the better part of a foot away from the title it belongs to.
        Box(
            modifier = modifier.fillMaxSize().then(shortcuts),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(modifier = Modifier.widthIn(max = LIST_MAX_WIDTH).fillMaxSize()) {
                ListHeader(
                    state = state,
                    onAction = onAction,
                    onScanToReceive = onScanToReceive,
                    searchFocusRequester = searchFocusRequester,
                )
                OrdersListBody(
                    state = state,
                    onAction = onAction,
                    onOrderClick = onOpenOrder,
                )
            }
        }
    }
}

/** The same cap the reading shelf and the order form use, so the tabs line up. */
private val LIST_MAX_WIDTH = 600.dp

/** A 56dp FAB, its 16dp margin, and a 16dp gap so the last row ends clear of it. */
private val FAB_CLEARANCE = 88.dp

@Composable
private fun ListHeader(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    onScanToReceive: () -> Unit,
    searchFocusRequester: FocusRequester,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        val picked = state.picked
        if (picked != null) {
            PickingHeader(count = picked.size, onAction = onAction)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(Res.string.orders_list_title),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onScanToReceive) {
                    Icon(
                        imageVector = TsundokuIcons.BarcodeScanner,
                        contentDescription = stringResource(Res.string.orders_list_scan_cd),
                    )
                }
                SortMenu(state = state, onAction = onAction)
            }
            SyncStatusLine(state = state, onAction = onAction)
        }
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
        // The chips carry counts; before the first load they would all read 0 and then jump.
        if (!state.isLoading) FilterChipsRow(state = state, onAction = onAction)
    }
}

/**
 * The title row while picking. Marking received is the one action on its own button — it is what a
 * parcel of several volumes calls for; the rest sit in the reading-state and overflow menus.
 */
@Composable
private fun PickingHeader(
    count: Int,
    onAction: (OrdersListAction) -> Unit,
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var moreExpanded by remember { mutableStateOf(false) }
    val hasPicks = count > 0
    SelectionHeader(
        count = count,
        onClose = { onAction(OrdersListAction.OnStopPicking) },
        onSelectAll = { onAction(OrdersListAction.OnPickAllShown) },
    ) {
        IconButton(
            onClick = { onAction(OrdersListAction.OnChangePicked(OrderChange.Status(OrderStatus.RECEIVED))) },
            enabled = hasPicks,
        ) {
            Icon(
                TsundokuIcons.CheckCircleOutline,
                contentDescription = stringResource(Res.string.selection_mark_received),
            )
        }
        ReadStateMenuButton(
            onSelect = { onAction(OrdersListAction.OnChangePicked(OrderChange.Reading(it))) },
            enabled = hasPicks,
        )
        Box {
            IconButton(onClick = { moreExpanded = true }, enabled = hasPicks) {
                Icon(TsundokuIcons.MoreVert, contentDescription = stringResource(Res.string.selection_more_cd))
            }
            DropdownMenu(expanded = moreExpanded, onDismissRequest = { moreExpanded = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.selection_mark_shipped)) },
                    onClick = {
                        moreExpanded = false
                        onAction(OrdersListAction.OnChangePicked(OrderChange.Status(OrderStatus.SHIPPED)))
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.selection_mark_cancelled)) },
                    onClick = {
                        moreExpanded = false
                        onAction(OrdersListAction.OnChangePicked(OrderChange.Status(OrderStatus.CANCELLED)))
                    },
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = stringResource(Res.string.selection_delete),
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        moreExpanded = false
                        confirmDelete = true
                    },
                )
            }
        }
    }
    if (confirmDelete) {
        DeleteSelectedConfirmDialog(
            count = count,
            onConfirm = {
                confirmDelete = false
                onAction(OrdersListAction.OnDeletePicked)
            },
            onDismiss = { confirmDelete = false },
        )
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
    // Clearing the filters returns the row to the start so it never reads as truncated.
    LaunchedEffect(state.isFiltered) {
        if (!state.isFiltered) scrollState.animateScrollTo(0)
    }
    FadingChipRow(scrollState = scrollState) {
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
}

@Composable
private fun OrdersListBody(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
    onOrderClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Until the local database has answered, nothing is known: showing "no orders" here would flash the
    // empty state on every cold start.
    if (state.isLoading) {
        ListSkeleton(modifier = modifier.fillMaxSize())
        return
    }
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
        OrdersLazyColumn(state = state, today = today, onAction = onAction, onOrderClick = onOrderClick)
    }
}

/**
 * Arrow keys walk the list and Enter opens the highlighted row; Space picks it and Ctrl/Cmd+A picks
 * everything shown — but only while the list itself holds focus, so the same keys still edit text
 * when the user is in the search field.
 */
@Composable
private fun Modifier.listKeyboardNavigation(
    state: OrdersListState,
    onAction: (OrdersListAction) -> Unit,
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

                // Space picks the highlighted order, starting picking if needed.
                Key.Spacebar -> {
                    displayed.getOrNull(index)?.let { onAction(OrdersListAction.OnTogglePicked(it.id)) } != null
                }

                Key.A -> {
                    if (event.isCommandOrControlPressed) onAction(OrdersListAction.OnPickAllShown)
                    event.isCommandOrControlPressed
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
    onAction: (OrdersListAction) -> Unit,
    onOrderClick: (String) -> Unit,
) {
    val picked = state.picked
    LazyColumn(
        modifier = Modifier.fillMaxSize().listKeyboardNavigation(state, onAction, onOrderClick),
        // The Add-order button floats over the bottom-right corner of this list; without room
        // below the last row it sat on that row's price and reading badge for good.
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = FAB_CLEARANCE),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        state.grouped.forEach { (status, orders) ->
            item(key = "header_${status.name}") {
                SectionHeader(label = stringResource(status.labelRes), count = orders.size)
            }
            items(items = orders, key = { it.id }) { order ->
                OrderRow(
                    order = order,
                    today = today,
                    onClick = {
                        if (picked != null) {
                            onAction(OrdersListAction.OnTogglePicked(order.id))
                        } else {
                            onOrderClick(order.id)
                        }
                    },
                    selected = order.id == state.selectedOrderId,
                    checked = picked?.let { order.id in it },
                    onLongClick = { onAction(OrdersListAction.OnTogglePicked(order.id)) },
                    estimate = state.estimates[order.id],
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
                onScanToReceive = {},
                onEditOrder = {},
                onReportDelay = {},
                snackbar = rememberSnackbarController(remember { SnackbarHostState() }),
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
