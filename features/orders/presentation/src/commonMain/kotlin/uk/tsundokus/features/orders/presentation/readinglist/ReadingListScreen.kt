package uk.tsundokus.features.orders.presentation.readinglist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_clear_search_cd
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_no_matches_title
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_search_placeholder
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_empty
import tsundokuapp.features.orders.presentation.generated.resources.reading_list_title
import tsundokuapp.features.orders.presentation.generated.resources.selection_mark_read
import tsundokuapp.features.orders.presentation.generated.resources.selection_start
import tsundokuapp.features.orders.presentation.generated.resources.selection_undo
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.components.SectionHeader
import uk.tsundokus.features.orders.presentation.components.StatusTile
import uk.tsundokus.features.orders.presentation.components.containerColor
import uk.tsundokus.features.orders.presentation.components.fullLabelRes
import uk.tsundokus.features.orders.presentation.components.labelRes
import uk.tsundokus.features.orders.presentation.components.onContainerColor
import uk.tsundokus.features.orders.presentation.components.pickableRowClicks
import uk.tsundokus.features.orders.presentation.selection.ReadStateMenuButton
import uk.tsundokus.features.orders.presentation.selection.SelectionBackHandler
import uk.tsundokus.features.orders.presentation.selection.SelectionHeader

@Composable
fun ReadingListRoot(
    onOpenOrder: (String) -> Unit,
    snackbar: SnackbarController,
    viewModel: ReadingListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ReadingListEvent.ShowMessage -> {
                snackbar.show(event.message)
            }

            is ReadingListEvent.ShowUndoableMessage -> {
                snackbar.show(event.message, UiText.Resource(Res.string.selection_undo), viewModel::onUndo)
            }
        }
    }

    ReadingListScreen(
        state = state,
        onCycleReadState = viewModel::onCycleReadState,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onOpenOrder = onOpenOrder,
        picking =
            ShelfPicking(
                onStart = viewModel::onStartPicking,
                onToggle = viewModel::onTogglePicked,
                onPickAllShown = viewModel::onPickAllShown,
                onStop = viewModel::onStopPicking,
                onSetReadState = viewModel::onSetPickedReadState,
            ),
    )
}

/** What picking on the shelf can do; grouped so the screen doesn't take five more callbacks. */
private class ShelfPicking(
    val onStart: () -> Unit,
    val onToggle: (String) -> Unit,
    val onPickAllShown: () -> Unit,
    val onStop: () -> Unit,
    val onSetReadState: (ReadState) -> Unit,
)

private val NoShelfPicking = ShelfPicking({}, {}, {}, {}, {})

private val SHELF_MAX_WIDTH = 600.dp

@Composable
private fun ReadingListScreen(
    state: ReadingListState,
    onCycleReadState: (String) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    picking: ShelfPicking,
    modifier: Modifier = Modifier,
) {
    SelectionBackHandler(isSelecting = state.isPicking, onClear = picking.onStop)
    // Capped and centred rather than stretched: at desktop width a shelf row would otherwise put
    // its title and its reading chip at opposite edges of the window.
    Box(
        modifier =
            modifier.fillMaxSize().onPreviewKeyEvent { event ->
                // The shell only acts on Escape where there is a screen to go back to.
                val leavesPicking =
                    event.type == KeyEventType.KeyDown && event.key == Key.Escape && state.isPicking
                if (leavesPicking) picking.onStop()
                leavesPicking
            },
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(modifier = Modifier.widthIn(max = SHELF_MAX_WIDTH).fillMaxWidth()) {
            val picked = state.picked
            if (picked != null) {
                SelectionHeader(
                    count = picked.size,
                    onClose = picking.onStop,
                    onSelectAll = picking.onPickAllShown,
                    // 56dp plus 2dp each side: the title row's height, so the list doesn't jump.
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                ) {
                    IconButton(
                        onClick = { picking.onSetReadState(ReadState.READ) },
                        enabled = picked.isNotEmpty(),
                    ) {
                        Icon(
                            TsundokuIcons.Check,
                            contentDescription = stringResource(Res.string.selection_mark_read),
                        )
                    }
                    ReadStateMenuButton(onSelect = picking.onSetReadState, enabled = picked.isNotEmpty())
                }
            } else {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(Res.string.reading_list_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f).padding(vertical = 8.dp),
                    )
                    IconButton(onClick = picking.onStart) {
                        Icon(
                            TsundokuIcons.Checklist,
                            contentDescription = stringResource(Res.string.selection_start),
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text(stringResource(Res.string.orders_list_search_placeholder)) },
                leadingIcon = { Icon(TsundokuIcons.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(
                                TsundokuIcons.Close,
                                contentDescription = stringResource(Res.string.orders_list_clear_search_cd),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            if (state.grouped.isEmpty()) {
                EmptyShelf(isFiltered = state.isFiltered, modifier = Modifier.fillMaxSize())
                return@Column
            }
            ShelfList(
                state = state,
                onCycleReadState = onCycleReadState,
                onOpenOrder = onOpenOrder,
                picking = picking,
            )
        }
    }
}

@Composable
private fun ShelfList(
    state: ReadingListState,
    onCycleReadState: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    picking: ShelfPicking,
) {
    val picked = state.picked
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
                    onClick = { if (picked != null) picking.onToggle(order.id) else onOpenOrder(order.id) },
                    onReadStateClick = { onCycleReadState(order.id) },
                    checked = picked?.let { order.id in it },
                    onLongClick = { picking.onToggle(order.id) },
                )
            }
        }
    }
}

@Composable
private fun ReadingRow(
    order: Order,
    onClick: () -> Unit,
    onReadStateClick: () -> Unit,
    checked: Boolean?,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    OutlinedCard(
        shape = shape,
        border =
            if (checked == true) {
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            } else {
                CardDefaults.outlinedCardBorder()
            },
        colors =
            if (checked == true) {
                CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            } else {
                CardDefaults.outlinedCardColors()
            },
        modifier =
            modifier
                .clip(shape)
                .pickableRowClicks(checked = checked, onClick = onClick, onLongClick = onLongClick),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (checked != null) {
                Checkbox(checked = checked, onCheckedChange = null)
                HorizontalSpacer(8.dp)
            }
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
            ReadStateChip(readState = order.readState, onClick = onReadStateClick, enabled = checked == null)
        }
    }
}

@Composable
private fun ReadStateChip(
    readState: ReadState,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    // Not tappable while picking: the row's tap picks it, and one order shouldn't change mid-pick.
    Surface(
        onClick = onClick,
        enabled = enabled,
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
private fun EmptyShelf(
    isFiltered: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text =
                stringResource(
                    if (isFiltered) {
                        Res.string.orders_list_no_matches_title
                    } else {
                        Res.string.reading_list_empty
                    },
                ),
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

private val previewShelf =
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
    )

@PreviewThemes
@Composable
private fun ReadingListScreenPreview() {
    TsundokuTheme {
        Surface {
            ReadingListScreen(
                state = ReadingListState(isLoading = false, grouped = previewShelf),
                onCycleReadState = {},
                onSearchQueryChange = {},
                onOpenOrder = {},
                picking = NoShelfPicking,
            )
        }
    }
}

@PreviewThemes
@Composable
private fun ReadingListPickingPreview() {
    TsundokuTheme {
        Surface {
            ReadingListScreen(
                state = ReadingListState(isLoading = false, grouped = previewShelf, picked = setOf("1")),
                onCycleReadState = {},
                onSearchQueryChange = {},
                onOpenOrder = {},
                picking = NoShelfPicking,
            )
        }
    }
}
