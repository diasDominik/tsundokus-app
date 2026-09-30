package uk.tsundokus.features.orders.presentation.serieslist

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.core.layout.WindowSizeClass.Companion.WIDTH_DP_EXPANDED_LOWER_BOUND
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.orders_list_clear_search_cd
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_placeholder_caption
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_placeholder_title
import tsundokuapp.features.orders.presentation.generated.resources.series_list_empty_caption
import tsundokuapp.features.orders.presentation.generated.resources.series_list_empty_title
import tsundokuapp.features.orders.presentation.generated.resources.series_list_no_matches_caption
import tsundokuapp.features.orders.presentation.generated.resources.series_list_no_matches_title
import tsundokuapp.features.orders.presentation.generated.resources.series_list_search_placeholder
import tsundokuapp.features.orders.presentation.generated.resources.series_list_sort
import tsundokuapp.features.orders.presentation.generated.resources.series_list_title
import tsundokuapp.features.orders.presentation.generated.resources.series_missing
import tsundokuapp.features.orders.presentation.generated.resources.series_missing_more
import tsundokuapp.features.orders.presentation.generated.resources.series_on_the_way
import tsundokuapp.features.orders.presentation.generated.resources.series_one_shot
import tsundokuapp.features.orders.presentation.generated.resources.series_volumes
import uk.tsundokus.core.designsystem.icon.TsundokuIcons
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.series.Series
import uk.tsundokus.features.orders.domain.series.toSeries
import uk.tsundokus.features.orders.presentation.addeditorder.OrderPrefill
import uk.tsundokus.features.orders.presentation.components.EmptyState
import uk.tsundokus.features.orders.presentation.components.SeriesThumbnail
import uk.tsundokus.features.orders.presentation.seriesdetail.SeriesDetailRoot

@Composable
fun SeriesListRoot(
    onOpenSeries: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    onOrderVolume: (OrderPrefill) -> Unit,
    snackbar: SnackbarController,
    viewModel: SeriesListViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SeriesListEvent.ShowMessage -> snackbar.show(event.message)
        }
    }

    SeriesListScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenSeries = onOpenSeries,
        onOpenOrder = onOpenOrder,
        onOrderVolume = onOrderVolume,
    )
}

/**
 * Two panes once the window is wide enough for both, as the Orders tab does: the list, and the
 * selected series beside it. Narrower, a series opens on its own screen.
 */
@Composable
private fun SeriesListScreen(
    state: SeriesListState,
    onAction: (SeriesListAction) -> Unit,
    onOpenSeries: (String) -> Unit,
    onOpenOrder: (String) -> Unit,
    onOrderVolume: (OrderPrefill) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isExpanded =
        currentWindowAdaptiveInfoV2().windowSizeClass.isWidthAtLeastBreakpoint(WIDTH_DP_EXPANDED_LOWER_BOUND)
    if (isExpanded) {
        Row(modifier = modifier.fillMaxSize()) {
            Column(modifier = Modifier.widthIn(min = 320.dp, max = 400.dp).fillMaxHeight()) {
                SeriesListContent(
                    state = state,
                    onAction = onAction,
                    onSeriesClick = { key -> onAction(SeriesListAction.OnSeriesSelected(key)) },
                )
            }
            VerticalDivider(modifier = Modifier.fillMaxHeight(), color = MaterialTheme.colorScheme.outlineVariant)
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
                val selectedKey = state.selectedKey
                if (selectedKey == null) {
                    EmptyState(
                        title = stringResource(Res.string.series_detail_placeholder_title),
                        caption = stringResource(Res.string.series_detail_placeholder_caption),
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    key(selectedKey) {
                        SeriesDetailRoot(
                            seriesKey = selectedKey,
                            onOpenOrder = onOpenOrder,
                            onOrderVolume = onOrderVolume,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
    } else {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(modifier = Modifier.widthIn(max = LIST_MAX_WIDTH).fillMaxSize()) {
                SeriesListContent(state = state, onAction = onAction, onSeriesClick = onOpenSeries)
            }
        }
    }
}

/** The same cap the other tabs use, so they line up. */
private val LIST_MAX_WIDTH = 600.dp

@Composable
private fun SeriesListContent(
    state: SeriesListState,
    onAction: (SeriesListAction) -> Unit,
    onSeriesClick: (String) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.series_list_title),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            SortMenu(state = state, onAction = onAction)
        }
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(SeriesListAction.OnSearchQueryChange(it)) },
            placeholder = { Text(stringResource(Res.string.series_list_search_placeholder)) },
            leadingIcon = { Icon(TsundokuIcons.Search, contentDescription = null) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(SeriesListAction.OnSearchQueryChange("")) }) {
                        Icon(
                            TsundokuIcons.Close,
                            contentDescription = stringResource(Res.string.orders_list_clear_search_cd),
                        )
                    }
                }
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
    }
    if (state.isLoading) return
    when {
        state.series.isEmpty() && state.isFiltered -> {
            EmptyState(
                title = stringResource(Res.string.series_list_no_matches_title),
                caption = stringResource(Res.string.series_list_no_matches_caption, state.searchQuery.trim()),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.series.isEmpty() -> {
            EmptyState(
                title = stringResource(Res.string.series_list_empty_title),
                caption = stringResource(Res.string.series_list_empty_caption),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        else -> {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items = state.series, key = Series::key) { series ->
                    SeriesRow(
                        series = series,
                        selected = series.key == state.selectedKey,
                        onClick = { onSeriesClick(series.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SortMenu(
    state: SeriesListState,
    onAction: (SeriesListAction) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton(onClick = { expanded = true }) {
            Text(stringResource(Res.string.series_list_sort, stringResource(state.sort.labelRes)))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SeriesSort.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(stringResource(sort.labelRes)) },
                    onClick = {
                        onAction(SeriesListAction.OnSortSelected(sort))
                        expanded = false
                    },
                    leadingIcon = {
                        if (sort == state.sort) Icon(TsundokuIcons.Check, contentDescription = null)
                    },
                )
            }
        }
    }
}

@Composable
private fun SeriesRow(
    series: Series,
    selected: Boolean,
    onClick: () -> Unit,
) {
    OutlinedCard(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        border =
            BorderStroke(
                1.dp,
                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SeriesThumbnail(coverIsbn = series.coverIsbn, status = series.status)
            HorizontalSpacer(12.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = series.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (series.author.isNotBlank()) {
                    Text(
                        text = series.author,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                SeriesSummary(series)
            }
        }
    }
}

/** "5 volumes · 1 on the way · missing 6", or "One-shot". Shared by the list rows and the detail header. */
@Composable
internal fun SeriesSummary(series: Series) {
    val parts =
        if (series.isOneShot) {
            listOf(stringResource(Res.string.series_one_shot))
        } else {
            listOfNotNull(
                pluralStringResource(Res.plurals.series_volumes, series.receivedCount, series.receivedCount),
                series.onTheWayCount
                    .takeIf { it > 0 }
                    ?.let { stringResource(Res.string.series_on_the_way, it) },
                series.missing
                    .takeIf { it.isNotEmpty() }
                    ?.let { stringResource(Res.string.series_missing, missingLabel(it)) },
            )
        }
    Text(
        text = parts.joinToString(" · "),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}

/** Up to three gaps by number, then how many more: "6, 9, 12 +4". */
@Composable
private fun missingLabel(missing: List<Int>): String {
    val shown = missing.take(MAX_MISSING_SHOWN).joinToString(", ")
    val more = missing.size - MAX_MISSING_SHOWN
    return if (more > 0) stringResource(Res.string.series_missing_more, shown, more) else shown
}

private const val MAX_MISSING_SHOWN = 3

@PreviewThemes
@Composable
private fun SeriesListScreenPreview() {
    val orders =
        (1..8).filter { it != 6 }.map { number ->
            Order(
                id = "op$number",
                title = "One Piece",
                author = "Eiichiro Oda",
                volume = "Vol. $number",
                status = if (number == 8) OrderStatus.SHIPPED else OrderStatus.RECEIVED,
                createdAt = number.toLong(),
            )
        } + Order(id = "one", title = "Look Back", author = "Tatsuki Fujimoto", status = OrderStatus.RECEIVED)
    TsundokuTheme {
        Surface {
            SeriesListScreen(
                state = SeriesListState(isLoading = false, series = orders.toSeries()),
                onAction = {},
                onOpenSeries = {},
                onOpenOrder = {},
                onOrderVolume = {},
            )
        }
    }
}
