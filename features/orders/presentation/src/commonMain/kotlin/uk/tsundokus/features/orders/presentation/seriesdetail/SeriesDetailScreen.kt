package uk.tsundokus.features.orders.presentation.seriesdetail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_extras
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_gone
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_order_next
import tsundokuapp.features.orders.presentation.generated.resources.series_detail_volumes
import tsundokuapp.features.orders.presentation.generated.resources.series_volume_missing_cd
import tsundokuapp.features.orders.presentation.generated.resources.series_volume_on_the_way_cd
import tsundokuapp.features.orders.presentation.generated.resources.series_volume_received_cd
import uk.tsundokus.core.designsystem.buttons.TsundokuButton
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.series.Series
import uk.tsundokus.features.orders.domain.series.SeriesVolume
import uk.tsundokus.features.orders.domain.series.VolumeState
import uk.tsundokus.features.orders.domain.series.toSeries
import uk.tsundokus.features.orders.presentation.addeditorder.OrderPrefill
import uk.tsundokus.features.orders.presentation.components.BookCover
import uk.tsundokus.features.orders.presentation.components.EmptyState
import uk.tsundokus.features.orders.presentation.components.OrderRow
import uk.tsundokus.features.orders.presentation.components.accentColor
import uk.tsundokus.features.orders.presentation.components.containerColor
import uk.tsundokus.features.orders.presentation.components.labelRes
import uk.tsundokus.features.orders.presentation.components.onContainerColor
import uk.tsundokus.features.orders.presentation.serieslist.SeriesSummary

@Composable
fun SeriesDetailRoot(
    seriesKey: String,
    onOpenOrder: (String) -> Unit,
    onOrderVolume: (OrderPrefill) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SeriesDetailViewModel =
        koinViewModel(
            key = seriesKey,
            parameters = { parametersOf(seriesKey) },
        ),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val series = state.series
    when {
        state.isLoading -> {
            Box(modifier = modifier)
        }

        series == null -> {
            EmptyState(title = stringResource(Res.string.series_detail_gone), caption = "", modifier = modifier)
        }

        else -> {
            SeriesDetailScreen(
                series = series,
                onOpenOrder = onOpenOrder,
                onOrderVolume = { onOrderVolume(series.nextOrderPrefill()) },
                modifier = modifier,
            )
        }
    }
}

/** The next volume of [this] series, filled in the way its latest volume was ordered. */
internal fun Series.nextOrderPrefill(): OrderPrefill =
    OrderPrefill(
        title = title,
        author = author,
        publisher = publisher,
        store = latest.store,
        volume = nextVolumeLabel.orEmpty(),
        currencyCode = latest.currency.code,
    )

@Composable
private fun SeriesDetailScreen(
    series: Series,
    onOpenOrder: (String) -> Unit,
    onOrderVolume: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.widthIn(max = DETAIL_MAX_WIDTH).fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SeriesHeader(series)
            if (series.isOneShot) {
                series.orders.forEach { order ->
                    OrderRow(order = order, today = todayIso(), onClick = { onOpenOrder(order.id) })
                }
            } else {
                SectionTitle(stringResource(Res.string.series_detail_volumes))
                VolumeStrip(series = series, onOpenOrder = onOpenOrder)
                if (series.extras.isNotEmpty()) {
                    SectionTitle(stringResource(Res.string.series_detail_extras))
                    series.extras.forEach { order ->
                        OrderRow(order = order, today = todayIso(), onClick = { onOpenOrder(order.id) })
                    }
                }
                series.nextVolume?.let { next ->
                    TsundokuButton(
                        text = stringResource(Res.string.series_detail_order_next, next),
                        onClick = onOrderVolume,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val DETAIL_MAX_WIDTH = 600.dp

@Composable
private fun SeriesHeader(series: Series) {
    Row(verticalAlignment = Alignment.Top) {
        series.coverIsbn?.let { isbn ->
            BookCover(isbn = isbn, width = 88.dp, modifier = Modifier.padding(end = 16.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = series.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )
            val byline = listOf(series.author, series.publisher).filter(String::isNotBlank).joinToString(" · ")
            if (byline.isNotEmpty()) {
                Text(
                    text = byline,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SeriesSummary(series)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
    )
}

/**
 * Every volume from the first to the last the user has: received ones filled, those on the way outlined
 * in their status colour, gaps dashed. Tapping one opens its order.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VolumeStrip(
    series: Series,
    onOpenOrder: (String) -> Unit,
) {
    val byNumber = remember(series) { series.volumes.associateBy(SeriesVolume::number) }
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        (series.volumes.first().number..series.volumes.last().number).forEach { number ->
            val volume = byNumber[number]
            if (volume == null) {
                MissingVolumeTile(number)
            } else {
                VolumeTile(volume = volume, onClick = { onOpenOrder(volume.latestOrder.id) })
            }
        }
    }
}

private val TILE_SIZE = 44.dp
private val TILE_SHAPE = RoundedCornerShape(10.dp)

@Composable
private fun VolumeTile(
    volume: SeriesVolume,
    onClick: () -> Unit,
) {
    val status = volume.latestOrder.status
    val description =
        when (volume.state) {
            VolumeState.RECEIVED -> {
                stringResource(Res.string.series_volume_received_cd, volume.number)
            }

            VolumeState.ON_THE_WAY -> {
                stringResource(
                    Res.string.series_volume_on_the_way_cd,
                    volume.number,
                    stringResource(status.labelRes),
                )
            }
        }
    val received = volume.state == VolumeState.RECEIVED
    Box(
        modifier =
            Modifier
                .size(TILE_SIZE)
                .clip(TILE_SHAPE)
                .background(if (received) OrderStatus.RECEIVED.containerColor() else Color.Transparent)
                .border(BorderStroke(2.dp, if (received) Color.Transparent else status.accentColor()), TILE_SHAPE)
                .clickable(onClick = onClick)
                .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = volume.number.toString(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (received) OrderStatus.RECEIVED.onContainerColor() else status.accentColor(),
        )
    }
}

@Composable
private fun MissingVolumeTile(number: Int) {
    val description = stringResource(Res.string.series_volume_missing_cd, number)
    val outline = MaterialTheme.colorScheme.outline
    Box(
        modifier =
            Modifier
                .size(TILE_SIZE)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = CornerRadius(10.dp.toPx()),
                        style =
                            Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f)),
                            ),
                    )
                }.semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = number.toString(),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@PreviewThemes
@Composable
private fun SeriesDetailScreenPreview() {
    val orders =
        (1..8).filter { it != 6 }.map { number ->
            Order(
                id = "$number",
                title = "One Piece",
                author = "Eiichiro Oda",
                volume = "Vol. $number",
                status = if (number == 8) OrderStatus.SHIPPED else OrderStatus.RECEIVED,
                createdAt = number.toLong(),
            )
        }
    TsundokuTheme {
        Surface {
            SeriesDetailScreen(series = orders.toSeries().single(), onOpenOrder = {}, onOrderVolume = {})
        }
    }
}
