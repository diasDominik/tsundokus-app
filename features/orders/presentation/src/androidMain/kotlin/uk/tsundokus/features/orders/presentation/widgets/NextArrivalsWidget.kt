package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
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
import androidx.glance.currentState
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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

/**
 * Everything the widget draws, words resolved. Which lines show depends on the widget's settings, so
 * that choice is made while drawing, and both messages are ready for it.
 */
internal data class NextArrivalsUi(
    val heading: String,
    val lines: List<ArrivalLine>,
    /** Signed out: shown instead of any list. */
    val signedOutMessage: String?,
    /** Shown when no line is left to show. */
    val nothingMessage: String,
) {
    /** The lines a widget with [style] shows: releases only if it asks for them. */
    fun linesFor(style: WidgetStyle): List<ArrivalLine> =
        if (style.showReleases) lines else lines.filter { it.phrase != ArrivalPhrase.RELEASES }
}

/**
 * The orders arriving next: as many as fit the widget's height, up to five. Launchers give a 2x2
 * cell very different heights, so the widget is drawn for its exact size rather than for fixed
 * steps. A tap on an order opens it, anywhere else opens the app.
 */
class NextArrivalsWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    // The picker's preview at a typical 2x2, not the minimum: there it shows a single arrival.
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetKind.NEXT_ARRIVALS.defaultSize))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshots = withContext(Dispatchers.IO) { WidgetSnapshotStore.snapshots(context) }
        val first = nextArrivalsUi(snapshots.value, todayIso())
        provideContent {
            // Both collected here, not read above: an open session redraws with each new snapshot,
            // and with each change saved in the widget's settings.
            val style = WidgetStyle.from(currentState<Preferences>())
            val snapshot by snapshots.collectAsState()
            val ui by produceState(first, snapshot) { value = nextArrivalsUi(snapshot, todayIso()) }
            WidgetTheme(style) { NextArrivalsContent(ui, style) }
        }
    }

    /** The widget picker's preview: made-up orders in the default look. */
    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val ui = nextArrivalsUi(previewSnapshot(todayIso()), todayIso())
        provideContent { WidgetTheme(WidgetStyle()) { NextArrivalsContent(ui) } }
    }

    internal companion object {
        /** The card's padding. */
        val PADDING_HEIGHT = 24.dp

        /** The heading and the gap under it. */
        val HEADING_HEIGHT = 16.dp

        /** One arrival: a line of title and a line of detail, with the gap before the next. */
        val ARRIVAL_HEIGHT = 46.dp

        /** The most shown however tall: what the snapshot keeps, and well inside Glance's 10 child slots. */
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
    val nothing = getString(Res.string.widget_nothing_on_the_way)
    if (!snapshot.signedIn) {
        return NextArrivalsUi(
            heading,
            emptyList(),
            getString(Res.string.widget_sign_in),
            nothing,
        )
    }
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
    return NextArrivalsUi(heading, lines, signedOutMessage = null, nothingMessage = nothing)
}

@Composable
internal fun NextArrivalsContent(
    ui: NextArrivalsUi,
    style: WidgetStyle = WidgetStyle(),
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val room =
        ((size.height - chromeHeight(style)) / NextArrivalsWidget.ARRIVAL_HEIGHT)
            .toInt()
            .coerceIn(1, NextArrivalsWidget.MAX_SHOWN)
    Column(GlanceModifier.widgetCard(style).clickable(actionStartActivity(openAppIntent(context)))) {
        if (style.showHeading) {
            WidgetHeading(ui.heading)
            Spacer(GlanceModifier.height(6.dp))
        }
        val shown = ui.linesFor(style).take(room)
        val message = ui.signedOutMessage ?: ui.nothingMessage.takeIf { shown.isEmpty() }
        if (message != null) {
            Text(
                text = message,
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.semantics { testTag = "message" },
            )
            return@Column
        }
        // A lone arrival gets two lines of title; in a list, each keeps to one.
        val titleLines = if (room == 1) 2 else 1
        // Glance lays a Row, Column or Box out in 10 pre-generated child slots and drops what
        // doesn't fit. The arrivals get a column of their own, spaced by padding rather than
        // spacers, so neither column comes near that whatever the count.
        Column {
            shown.forEachIndexed { index, line ->
                ArrivalLineView(line, titleLines, context, topGap = if (index == 0) 0.dp else 8.dp)
            }
        }
    }
}

@Composable
private fun ArrivalLineView(
    line: ArrivalLine,
    titleLines: Int,
    context: Context,
    topGap: Dp,
) {
    Column(
        GlanceModifier
            .fillMaxWidth()
            .padding(top = topGap)
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

/** What the card uses before the first arrival: its padding, and the heading when shown. */
private fun chromeHeight(style: WidgetStyle) =
    NextArrivalsWidget.PADDING_HEIGHT + if (style.showHeading) NextArrivalsWidget.HEADING_HEIGHT else 0.dp
