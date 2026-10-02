package uk.tsundokus.features.orders.presentation.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.series_volumes
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_on_order
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_owned
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_read
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_read_share_cd
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_series
import tsundokuapp.features.orders.presentation.generated.resources.stats_collection_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_days
import tsundokuapp.features.orders.presentation.generated.resources.stats_delivery_caption
import tsundokuapp.features.orders.presentation.generated.resources.stats_delivery_sample
import tsundokuapp.features.orders.presentation.generated.resources.stats_delivery_shipping
import tsundokuapp.features.orders.presentation.generated.resources.stats_delivery_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_delivery_wait
import tsundokuapp.features.orders.presentation.generated.resources.stats_empty_caption
import tsundokuapp.features.orders.presentation.generated.resources.stats_empty_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_all_read
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_oldest
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_reading
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_unread
import tsundokuapp.features.orders.presentation.generated.resources.stats_reliability_caption
import tsundokuapp.features.orders.presentation.generated.resources.stats_reliability_late
import tsundokuapp.features.orders.presentation.generated.resources.stats_reliability_slip
import tsundokuapp.features.orders.presentation.generated.resources.stats_reliability_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_spend_average
import tsundokuapp.features.orders.presentation.generated.resources.stats_spend_none
import tsundokuapp.features.orders.presentation.generated.resources.stats_spend_on_the_way
import tsundokuapp.features.orders.presentation.generated.resources.stats_spend_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_top_publishers_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_top_stores_title
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.HorizontalSpacer
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.presentation.util.ObserveAsEvents
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.stats.CollectionCounts
import uk.tsundokus.features.orders.domain.stats.CurrencySpend
import uk.tsundokus.features.orders.domain.stats.DeliveryStats
import uk.tsundokus.features.orders.domain.stats.MIN_ORDERS_FOR_RELIABILITY
import uk.tsundokus.features.orders.domain.stats.PileStats
import uk.tsundokus.features.orders.domain.stats.RankedName
import uk.tsundokus.features.orders.domain.stats.StatsPeriod
import uk.tsundokus.features.orders.domain.stats.StoreReliability
import uk.tsundokus.features.orders.domain.stats.toCollectionStats
import uk.tsundokus.features.orders.presentation.components.EmptyState
import uk.tsundokus.features.orders.presentation.components.amountLabel

@Composable
fun StatsRoot(
    onOpenReading: () -> Unit,
    snackbar: SnackbarController,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is StatsEvent.ShowMessage -> snackbar.show(event.message)
        }
    }

    StatsScreen(
        state = state,
        onAction = viewModel::onAction,
        onOpenReading = onOpenReading,
    )
}

/** Cards in as many columns as fit: one on a phone, two or three on a tablet or desktop. */
@Composable
private fun StatsScreen(
    state: StatsState,
    onAction: (StatsAction) -> Unit,
    onOpenReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stats = state.stats
    if (state.isLoading || stats == null) return
    if (stats.isEmpty) {
        Column(modifier = modifier.fillMaxSize()) {
            StatsTitle(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            EmptyState(
                title = stringResource(Res.string.stats_empty_title),
                caption = stringResource(Res.string.stats_empty_caption),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        return
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(CARD_MIN_WIDTH),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalItemSpacing = 12.dp,
        modifier = modifier.fillMaxSize(),
    ) {
        item(key = "header", span = StaggeredGridItemSpan.FullLine) {
            Column {
                StatsTitle()
                VerticalSpacer(8.dp)
                PeriodPicker(selected = state.period, onSelect = { onAction(StatsAction.OnPeriodSelected(it)) })
            }
        }
        item(key = "pile") { PileCard(pile = stats.pile, onClick = onOpenReading) }
        item(key = "collection") { CollectionCard(collection = stats.collection) }
        item(key = "spend") { SpendCard(spend = stats.spend) }
        if (stats.delivery.shippingSample > 0 || stats.delivery.waitSample > 0) {
            item(key = "delivery") { DeliveryCard(delivery = stats.delivery) }
        }
        if (stats.storeReliability.isNotEmpty()) {
            item(key = "reliability") { ReliabilityCard(stores = stats.storeReliability) }
        }
        if (stats.topStores.isNotEmpty()) {
            item(key = "stores") {
                RankedCard(title = stringResource(Res.string.stats_top_stores_title), names = stats.topStores)
            }
        }
        if (stats.topPublishers.isNotEmpty()) {
            item(key = "publishers") {
                RankedCard(
                    title = stringResource(Res.string.stats_top_publishers_title),
                    names = stats.topPublishers,
                )
            }
        }
    }
}

/** Narrow enough for one column on any phone, wide enough for the 12-bar chart. */
private val CARD_MIN_WIDTH = 320.dp

@Composable
private fun StatsTitle(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.stats_title),
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodPicker(
    selected: StatsPeriod,
    onSelect: (StatsPeriod) -> Unit,
) {
    val periods = StatsPeriod.entries
    SingleChoiceSegmentedButtonRow(modifier = Modifier.widthIn(max = 480.dp).fillMaxWidth()) {
        periods.forEachIndexed { index, period ->
            SegmentedButton(
                selected = period == selected,
                onClick = { onSelect(period) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = periods.size),
            ) {
                Text(stringResource(period.labelRes), maxLines = 1)
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    OutlinedCard(shape = RoundedCornerShape(16.dp), modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            VerticalSpacer(12.dp)
            content()
        }
    }
}

/** The tsundoku itself, so it stands out; a tap opens the Reading tab, where the pile is. */
@Composable
private fun PileCard(
    pile: PileStats,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors =
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(Res.string.stats_pile_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            VerticalSpacer(8.dp)
            if (pile.unread == 0) {
                Text(
                    text = stringResource(Res.string.stats_pile_all_read),
                    style = MaterialTheme.typography.bodyLarge,
                )
                return@Column
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = pile.unread.toString(),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalSpacer(8.dp)
                Text(
                    text = pluralStringResource(Res.plurals.stats_pile_unread, pile.unread),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }
            if (pile.reading > 0) {
                Text(
                    text = stringResource(Res.string.stats_pile_reading, pile.reading),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            pile.oldestWaitingDays?.let { days ->
                Text(
                    text = pluralStringResource(Res.plurals.stats_pile_oldest, days, days),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun CollectionCard(collection: CollectionCounts) {
    StatCard(title = stringResource(Res.string.stats_collection_title)) {
        Row {
            BigNumber(
                value = collection.owned,
                label = stringResource(Res.string.stats_collection_owned),
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = collection.read,
                label = stringResource(Res.string.stats_collection_read),
                modifier = Modifier.weight(1f),
            )
        }
        if (collection.owned > 0) {
            VerticalSpacer(8.dp)
            ProportionBar(
                fraction = collection.read.toFloat() / collection.owned,
                color = MaterialTheme.colorScheme.tertiary,
                description =
                    stringResource(Res.string.stats_collection_read_share_cd, collection.read, collection.owned),
            )
        }
        VerticalSpacer(16.dp)
        Row {
            BigNumber(
                value = collection.series,
                label = stringResource(Res.string.stats_collection_series),
                modifier = Modifier.weight(1f),
            )
            BigNumber(
                value = collection.ordered - collection.owned,
                label = stringResource(Res.string.stats_collection_on_order),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun BigNumber(
    value: Int,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * The user's own currency gets the total and the chart; any other currency they buy in gets a line
 * underneath. Amounts are never converted, so they are never added together.
 */
@Composable
private fun SpendCard(spend: List<CurrencySpend>) {
    StatCard(title = stringResource(Res.string.stats_spend_title)) {
        val main = spend.firstOrNull()
        if (main == null) {
            Text(
                text = stringResource(Res.string.stats_spend_none),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@StatCard
        }
        Text(
            text = amountLabel(main.total, main.currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = spendSummary(main),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        // Bars all at zero (only unpriced orders) say nothing a "0.00" total doesn't.
        if (main.buckets.any { it.amount > 0.0 }) {
            VerticalSpacer(16.dp)
            SpendBarChart(buckets = main.buckets, currency = main.currency)
        }
        spend.drop(1).forEach { other ->
            VerticalSpacer(12.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = amountLabel(other.total, other.currency),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                HorizontalSpacer(8.dp)
                Text(
                    text = spendSummary(other),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** "4 volumes · 9.50 € per volume · 19.00 € on the way", leaving out what doesn't apply. */
@Composable
private fun spendSummary(spend: CurrencySpend): String =
    listOfNotNull(
        pluralStringResource(Res.plurals.series_volumes, spend.volumes, spend.volumes),
        spend.averagePrice?.let {
            stringResource(
                Res.string.stats_spend_average,
                amountLabel(it, spend.currency),
            )
        },
        spend.onTheWay
            .takeIf { it > 0.0 }
            ?.let { stringResource(Res.string.stats_spend_on_the_way, amountLabel(it, spend.currency)) },
    ).joinToString(" · ")

@Composable
private fun DeliveryCard(delivery: DeliveryStats) {
    StatCard(title = stringResource(Res.string.stats_delivery_title)) {
        delivery.medianShippingDays?.let { days ->
            DeliveryRow(
                label = stringResource(Res.string.stats_delivery_shipping),
                days = days,
                sample = delivery.shippingSample,
            )
        }
        delivery.medianWaitDays?.let { days ->
            DeliveryRow(
                label = stringResource(Res.string.stats_delivery_wait),
                days = days,
                sample = delivery.waitSample,
            )
        }
        VerticalSpacer(4.dp)
        Text(
            text = stringResource(Res.string.stats_delivery_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeliveryRow(
    label: String,
    days: Int,
    sample: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = pluralStringResource(Res.plurals.stats_delivery_sample, sample, sample),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = pluralStringResource(Res.plurals.stats_days, days, days),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun ReliabilityCard(stores: List<StoreReliability>) {
    StatCard(title = stringResource(Res.string.stats_reliability_title)) {
        stores.forEach { store ->
            Column(modifier = Modifier.padding(bottom = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = store.store,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalSpacer(8.dp)
                    Text(
                        text = stringResource(Res.string.stats_reliability_late, store.delayed, store.orders),
                        style = MaterialTheme.typography.labelLarge,
                        color =
                            if (store.delayed > 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
                store.medianSlipDays?.let { days ->
                    Text(
                        text = pluralStringResource(Res.plurals.stats_reliability_slip, days, days),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalSpacer(4.dp)
                ProportionBar(fraction = store.delayRate.toFloat(), color = MaterialTheme.colorScheme.error)
            }
        }
        Text(
            text = stringResource(Res.string.stats_reliability_caption, MIN_ORDERS_FOR_RELIABILITY),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RankedCard(
    title: String,
    names: List<RankedName>,
) {
    val most = names.maxOf(RankedName::count)
    StatCard(title = title) {
        names.forEach { ranked ->
            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = ranked.name,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    HorizontalSpacer(8.dp)
                    Text(
                        text = pluralStringResource(Res.plurals.series_volumes, ranked.count, ranked.count),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                VerticalSpacer(4.dp)
                ProportionBar(fraction = ranked.count.toFloat() / most, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

/**
 * A thin bar filled to [fraction] over a faint track. Read out as [description] when given; otherwise
 * it only repeats the numbers beside it, so screen readers skip it.
 */
@Composable
private fun ProportionBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest, RoundedCornerShape(3.dp))
                .semantics { if (description != null) contentDescription = description },
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(fraction.coerceIn(0f, 1f))
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp)),
        )
    }
}

private fun previewOrder(
    title: String,
    status: OrderStatus = OrderStatus.RECEIVED,
    readState: ReadState = ReadState.WANT,
    store: String = "Amazon",
    orderDate: String,
    price: Double = 9.99,
    delayedTo: String = "",
) = Order(
    id = "$title-$orderDate",
    title = title,
    publisher = "Carlsen",
    store = store,
    price = price,
    status = status,
    readState = readState,
    orderDate = orderDate,
    shipDate = orderDate,
    eta = orderDate,
    receivedDate = if (status == OrderStatus.RECEIVED) "2026-09-28" else "",
    delayedTo = delayedTo,
)

private val previewOrders =
    listOf(
        previewOrder("One Piece 104", orderDate = "2026-01-12", readState = ReadState.READ),
        previewOrder("One Piece 105", orderDate = "2026-03-02", readState = ReadState.READING),
        previewOrder("Berserk 41", orderDate = "2026-05-20", store = "Thalia", delayedTo = "2026-06-10"),
        previewOrder("Berserk 42", orderDate = "2026-07-14", store = "Thalia"),
        previewOrder("Vagabond 37", orderDate = "2026-08-01", store = "Thalia", price = 14.0),
        previewOrder("Frieren 13", orderDate = "2026-09-18", status = OrderStatus.SHIPPED),
        previewOrder("Dandadan 18", orderDate = "2026-09-30", status = OrderStatus.ORDERED, price = 7.5),
    )

@PreviewThemes
@Composable
private fun StatsScreenPreview() {
    TsundokuTheme {
        Surface {
            StatsScreen(
                state =
                    StatsState(
                        isLoading = false,
                        stats =
                            previewOrders.toCollectionStats(
                                "2026-10-02",
                                StatsPeriod.LAST_12_MONTHS,
                                AppCurrency.EUR,
                            ),
                    ),
                onAction = {},
                onOpenReading = {},
            )
        }
    }
}

@PreviewThemes
@Composable
private fun StatsScreenEmptyPreview() {
    TsundokuTheme {
        Surface {
            StatsScreen(
                state =
                    StatsState(
                        isLoading = false,
                        stats =
                            emptyList<Order>().toCollectionStats(
                                "2026-10-02",
                                StatsPeriod.LAST_12_MONTHS,
                                AppCurrency.EUR,
                            ),
                    ),
                onAction = {},
                onOpenReading = {},
            )
        }
    }
}
