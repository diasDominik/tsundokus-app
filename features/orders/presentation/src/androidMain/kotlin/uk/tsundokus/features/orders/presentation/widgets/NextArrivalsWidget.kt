package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getString
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.widget_arrives
import tsundokuapp.features.orders.presentation.generated.resources.widget_delayed_to
import tsundokuapp.features.orders.presentation.generated.resources.widget_next_arrivals_title
import tsundokuapp.features.orders.presentation.generated.resources.widget_nothing_on_the_way
import tsundokuapp.features.orders.presentation.generated.resources.widget_overdue
import tsundokuapp.features.orders.presentation.generated.resources.widget_releases
import tsundokuapp.features.orders.presentation.generated.resources.widget_sign_in
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.presentation.navigation.orderDeepLink

/** One arrival as the widget shows it, with its words already resolved. */
internal data class ArrivalLine(
    val id: String,
    val title: String,
    val volume: String,
    val detail: String,
    val phrase: ArrivalPhrase,
)

/** Everything the widget draws: [message] stands in for the list when there is nothing to list. */
internal data class NextArrivalsUi(
    val heading: String,
    val lines: List<ArrivalLine>,
    val message: String?,
)

/**
 * The orders arriving next: as many as fit the widget's height, up to five. Launchers give a 2x2
 * cell very different heights, so the widget is drawn for its exact size rather than for fixed
 * steps. A tap on an order opens it, anywhere else opens the app.
 */
class NextArrivalsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshots = withContext(Dispatchers.IO) { WidgetSnapshotStore.snapshots(context) }
        val first = nextArrivalsUi(snapshots.value, todayIso())
        provideContent {
            // Collected here, not read above: an open session redraws with each new snapshot.
            val snapshot by snapshots.collectAsState()
            val ui by produceState(first, snapshot) { value = nextArrivalsUi(snapshot, todayIso()) }
            GlanceTheme { NextArrivalsContent(ui) }
        }
    }

    internal companion object {
        /** The heading and the card's padding. */
        val CHROME_HEIGHT = 40.dp

        /** One arrival: a line of title and a line of detail, with the gap before the next. */
        val ARRIVAL_HEIGHT = 46.dp

        const val MAX_SHOWN = 5
    }
}

class NextArrivalsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextArrivalsWidget()
}

internal suspend fun nextArrivalsUi(
    snapshot: WidgetSnapshot,
    today: String,
): NextArrivalsUi {
    val heading = getString(Res.string.widget_next_arrivals_title)
    if (!snapshot.signedIn) return NextArrivalsUi(heading, emptyList(), getString(Res.string.widget_sign_in))
    val lines =
        snapshot.arrivalsOn(today).map { arrival ->
            val phrase = arrival.phraseOn(today)
            val day = widgetDay(arrival.date, today)
            ArrivalLine(
                id = arrival.id,
                title = arrival.title,
                volume = arrival.volume,
                detail =
                    when (phrase) {
                        ArrivalPhrase.RELEASES -> getString(Res.string.widget_releases, day)
                        ArrivalPhrase.ARRIVES -> getString(Res.string.widget_arrives, day)
                        ArrivalPhrase.DELAYED_TO -> getString(Res.string.widget_delayed_to, day)
                        ArrivalPhrase.OVERDUE -> getString(Res.string.widget_overdue, day)
                    },
                phrase = phrase,
            )
        }
    val message = if (lines.isEmpty()) getString(Res.string.widget_nothing_on_the_way) else null
    return NextArrivalsUi(heading, lines, message)
}

@Composable
internal fun NextArrivalsContent(ui: NextArrivalsUi) {
    val context = LocalContext.current
    val size = LocalSize.current
    val room =
        ((size.height - NextArrivalsWidget.CHROME_HEIGHT) / NextArrivalsWidget.ARRIVAL_HEIGHT)
            .toInt()
            .coerceIn(1, NextArrivalsWidget.MAX_SHOWN)
    Column(GlanceModifier.widgetCard().clickable(actionStartActivity(openAppIntent(context)))) {
        WidgetHeading(ui.heading)
        Spacer(GlanceModifier.height(6.dp))
        if (ui.message != null) {
            Text(
                text = ui.message,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.semantics { testTag = "message" },
            )
            return@Column
        }
        // A lone arrival gets two lines of title; in a list, each keeps to one.
        val titleLines = if (room == 1) 2 else 1
        ui.lines.take(room).forEachIndexed { index, line ->
            if (index > 0) Spacer(GlanceModifier.height(8.dp))
            ArrivalLineView(line, titleLines, context)
        }
    }
}

@Composable
private fun ArrivalLineView(
    line: ArrivalLine,
    titleLines: Int,
    context: Context,
) {
    Column(
        GlanceModifier
            .fillMaxWidth()
            .clickable(actionStartActivity(openAppIntent(context, orderDeepLink(line.id))))
            .semantics { testTag = "arrival-${line.id}" },
    ) {
        Text(
            text = listOf(line.title, line.volume).filter { it.isNotBlank() }.joinToString(" "),
            style =
                TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = GlanceTheme.colors.onSurface,
                ),
            maxLines = titleLines,
        )
        Text(
            text = line.detail,
            style = TextStyle(fontSize = 12.sp, color = line.phrase.tint()),
            maxLines = 1,
        )
    }
}

/** The same colours the app gives these states: late in error red, on the way in the main colour. */
@Composable
private fun ArrivalPhrase.tint(): ColorProvider =
    when (this) {
        ArrivalPhrase.RELEASES -> GlanceTheme.colors.secondary
        ArrivalPhrase.ARRIVES -> GlanceTheme.colors.primary
        ArrivalPhrase.DELAYED_TO, ArrivalPhrase.OVERDUE -> GlanceTheme.colors.error
    }
