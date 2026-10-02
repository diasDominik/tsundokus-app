package uk.tsundokus.features.orders.presentation.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.stats_spend_bar
import uk.tsundokus.core.designsystem.preview.PreviewThemes
import uk.tsundokus.core.designsystem.spacer.VerticalSpacer
import uk.tsundokus.core.designsystem.theme.TsundokuTheme
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.presentation.date.formatNarrowMonth
import uk.tsundokus.core.presentation.date.formatShortMonthYear
import uk.tsundokus.features.orders.domain.stats.SpendBucket
import uk.tsundokus.features.orders.presentation.components.amountLabel

/**
 * Spend as a bar per month (or year). The newest bar with any spend is picked to start with; tapping
 * another picks it, and the picked bar's amount shows above the chart.
 */
@Composable
internal fun SpendBarChart(
    buckets: List<SpendBucket>,
    currency: AppCurrency,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty()) return
    var selected by remember(buckets) {
        mutableIntStateOf(buckets.indexOfLast { it.amount > 0.0 }.takeIf { it >= 0 } ?: buckets.lastIndex)
    }
    val bucketName = { bucket: SpendBucket ->
        bucket.month?.let { formatShortMonthYear(bucket.year, it) } ?: bucket.year.toString()
    }
    val description =
        buckets
            .map {
                stringResource(Res.string.stats_spend_bar, bucketName(it), amountLabel(it.amount, currency))
            }.joinToString(", ")
    val highest = buckets.maxOf(SpendBucket::amount)

    Column(modifier = modifier) {
        Text(
            text =
                stringResource(
                    Res.string.stats_spend_bar,
                    bucketName(buckets[selected]),
                    amountLabel(buckets[selected].amount, currency),
                ),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        VerticalSpacer(8.dp)
        val pickedColor = MaterialTheme.colorScheme.primary
        val otherColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        val emptyColor = MaterialTheme.colorScheme.outlineVariant
        Canvas(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(CHART_HEIGHT)
                    .semantics { contentDescription = description }
                    .pointerInput(buckets) {
                        detectTapGestures { offset ->
                            selected = (offset.x / size.width * buckets.size).toInt().coerceIn(buckets.indices)
                        }
                    },
        ) {
            val slot = size.width / buckets.size
            val barWidth = slot * BAR_SHARE
            val radius = CornerRadius(minOf(barWidth / 2, 4.dp.toPx()))
            buckets.forEachIndexed { index, bucket ->
                val left = index * slot + (slot - barWidth) / 2
                // Every bar shows something, so a month with no spend still reads as a month.
                val height =
                    if (highest > 0.0 && bucket.amount > 0.0) {
                        maxOf((bucket.amount / highest * size.height).toFloat(), 2.dp.toPx())
                    } else {
                        2.dp.toPx()
                    }
                drawRoundRect(
                    color =
                        when {
                            index == selected -> pickedColor
                            bucket.amount > 0.0 -> otherColor
                            else -> emptyColor
                        },
                    topLeft = Offset(left, size.height - height),
                    size = Size(barWidth, height),
                    cornerRadius = radius,
                )
            }
        }
        VerticalSpacer(4.dp)
        Row(modifier = Modifier.fillMaxWidth()) {
            buckets.forEachIndexed { index, bucket ->
                Text(
                    text = bucket.month?.let(::formatNarrowMonth) ?: bucket.year.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color =
                        if (index == selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

private val CHART_HEIGHT = 112.dp

/** How much of its slot a bar fills; the rest is the gap between bars. */
private const val BAR_SHARE = 0.6f

@PreviewThemes
@Composable
private fun SpendBarChartPreview() {
    TsundokuTheme {
        Surface {
            SpendBarChart(
                buckets =
                    listOf(0.0, 23.5, 41.0, 8.99, 0.0, 64.2, 31.0, 12.0, 0.0, 55.4, 18.0, 27.5).mapIndexed {
                        index,
                        amount,
                        ->
                        SpendBucket(
                            year =
                                if (index <
                                    2
                                ) {
                                    2025
                                } else {
                                    2026
                                },
                            month = (index + 10) % 12 + 1,
                            amount = amount,
                        )
                    },
                currency = AppCurrency.EUR,
            )
        }
    }
}
