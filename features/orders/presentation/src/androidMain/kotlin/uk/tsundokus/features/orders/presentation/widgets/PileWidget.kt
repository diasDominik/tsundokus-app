package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.width
import androidx.glance.semantics.semantics
import androidx.glance.semantics.testTag
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.getPluralString
import org.jetbrains.compose.resources.getString
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_reading
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_title
import tsundokuapp.features.orders.presentation.generated.resources.stats_pile_unread
import tsundokuapp.features.orders.presentation.generated.resources.widget_all_caught_up
import tsundokuapp.features.orders.presentation.generated.resources.widget_oldest_waiting
import tsundokuapp.features.orders.presentation.generated.resources.widget_sign_in
import uk.tsundokus.features.orders.domain.dates.todayIso
import kotlin.math.min
import kotlin.math.sqrt

/** The pile as the widget shows it. [count] is null when [message] stands in for it. */
internal data class PileUi(
    val heading: String,
    val count: Int?,
    val countLabel: String,
    /** "2 being read · oldest 30 days", or null when neither applies. */
    val detail: String?,
    val message: String?,
    /** The volumes to draw, bottom of the pile first. */
    val books: List<WidgetPileBook> = emptyList(),
    /** The day the dust is worked out for. */
    val today: String = "",
)

/**
 * The tsundoku: how many arrived volumes wait to be read, and, where the height allows, a heading
 * and how many are being read and how long the oldest has waited. From about 2x2 the pile itself is
 * drawn too, gathering dust (see [PileArt]). Drawn for its exact size, since launchers give a 2x1
 * cell very different heights. A tap opens the app.
 */
class PileWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    // The picker's preview at a typical 2x1, not the minimum: there it has no heading or details.
    override val previewSizeMode = SizeMode.Responsive(setOf(WidgetKind.PILE.defaultSize))

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshots = withContext(Dispatchers.IO) { WidgetSnapshotStore.snapshots(context) }
        val first = pileUi(snapshots.value, todayIso())
        provideContent {
            // Both collected here, not read above: an open session redraws with each new snapshot,
            // and with each change saved in the widget's settings.
            val style = WidgetStyle.from(currentState<Preferences>())
            val snapshot by snapshots.collectAsState()
            val ui by produceState(first, snapshot) { value = pileUi(snapshot, todayIso()) }
            WidgetTheme(style) { PileContent(ui, style) }
        }
    }

    /** The widget picker's preview: a made-up pile in the default look. */
    override suspend fun providePreview(
        context: Context,
        widgetCategory: Int,
    ) {
        val ui = pileUi(previewSnapshot(todayIso()), todayIso())
        provideContent { WidgetTheme(WidgetStyle()) { PileContent(ui) } }
    }

    internal companion object {
        /** Tall enough for the heading above the count. */
        val HEADING_FROM = 84.dp

        /** Tall enough for the heading, the count and the detail line under it. */
        val DETAIL_FROM = 104.dp

        /** Tall enough for a pile worth drawing next to the count: a 2x2 cell on most launchers. */
        val PILE_FROM = 140.dp
    }
}

class PileWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PileWidget()
}

internal suspend fun pileUi(
    snapshot: WidgetSnapshot,
    today: String,
): PileUi {
    val heading = getString(Res.string.stats_pile_title)
    val message =
        when {
            !snapshot.signedIn -> getString(Res.string.widget_sign_in)
            snapshot.unread == 0 -> getString(Res.string.widget_all_caught_up)
            else -> null
        }
    if (message != null) return PileUi(heading, count = null, countLabel = "", detail = null, message = message)
    val detail =
        listOfNotNull(
            snapshot.reading.takeIf { it > 0 }?.let { getString(Res.string.stats_pile_reading, it) },
            snapshot.oldestWaitingDays(today)?.let { getPluralString(Res.plurals.widget_oldest_waiting, it, it) },
        ).joinToString(" · ").ifEmpty { null }
    return PileUi(
        heading = heading,
        count = snapshot.unread,
        countLabel = getPluralString(Res.plurals.stats_pile_unread, snapshot.unread),
        detail = detail,
        message = null,
        books = snapshot.pile,
        today = today,
    )
}

@Composable
internal fun PileContent(
    ui: PileUi,
    style: WidgetStyle = WidgetStyle(),
) {
    val context = LocalContext.current
    val height = LocalSize.current.height
    if (ui.count != null && ui.books.isNotEmpty() && height >= PileWidget.PILE_FROM) {
        PileWithArt(ui, ui.count, style)
        return
    }
    Column(
        modifier = GlanceModifier.widgetCard(style).clickable(actionStartActivity(openAppIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (style.showHeading && height >= PileWidget.HEADING_FROM) WidgetHeading(ui.heading)
        if (ui.count == null) {
            Text(
                text = ui.message.orEmpty(),
                style = TextStyle(fontSize = 14.sp, color = GlanceTheme.colors.onSurfaceVariant),
                modifier = GlanceModifier.semantics { testTag = "message" },
            )
            return@Column
        }
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = ui.count.toString(),
                style =
                    TextStyle(
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold,
                        color = GlanceTheme.colors.onSurface,
                    ),
                modifier = GlanceModifier.semantics { testTag = "count" },
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(
                text = ui.countLabel,
                style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
            )
        }
        if (style.showDetail && ui.detail != null && height >= PileWidget.DETAIL_FROM) {
            Text(
                text = ui.detail,
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
                modifier = GlanceModifier.semantics { testTag = "detail" },
            )
        }
    }
}

/**
 * The pile drawn under the words: the picture fills the card, and [PileArt] keeps the pile below
 * the room the heading, count and detail line take at the top.
 */
@Composable
private fun PileWithArt(
    ui: PileUi,
    count: Int,
    style: WidgetStyle,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    // The card's own padding comes off the picture's room.
    val width = size.width - WIDGET_CARD_PADDING * 2
    val height = size.height - WIDGET_CARD_PADDING * 2
    val showHeading = style.showHeading
    val detail = ui.detail?.takeIf { style.showDetail }
    val words =
        (if (showHeading) HEADING_HEIGHT else 0.dp) + COUNT_HEIGHT + (if (detail != null) DETAIL_HEIGHT else 0.dp)
    val art =
        remember(ui.books, ui.today, width, height, words) {
            val scale = artScale(width.value, height.value, context.resources.displayMetrics.density)
            PileArt.draw(
                widthPx = (width.value * scale).toInt(),
                heightPx = (height.value * scale).toInt(),
                density = scale,
                books = ui.books,
                today = ui.today,
                top = (words + WORDS_GAP).value * scale,
            )
        }
    Box(modifier = GlanceModifier.widgetCard(style).clickable(actionStartActivity(openAppIntent(context)))) {
        Image(
            provider = ImageProvider(art),
            // The words above it say what it shows.
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = GlanceModifier.fillMaxSize(),
        )
        Column {
            if (showHeading) WidgetHeading(ui.heading)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = count.toString(),
                    style =
                        TextStyle(
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Bold,
                            color = GlanceTheme.colors.onSurface,
                        ),
                    modifier = GlanceModifier.semantics { testTag = "count" },
                )
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    text = ui.countLabel,
                    style = TextStyle(fontSize = 13.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                )
            }
            if (detail != null) {
                Text(
                    text = detail,
                    style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                    maxLines = 1,
                    modifier = GlanceModifier.semantics { testTag = "detail" },
                )
            }
        }
    }
}

// How tall each line of words is at the default font size, to keep the pile out from under them.
// A larger font only lets the words overlap the top of the pile a little.
private val HEADING_HEIGHT = 18.dp
private val COUNT_HEIGHT = 38.dp
private val DETAIL_HEIGHT = 16.dp
private val WORDS_GAP = 6.dp

/**
 * Widget pictures travel to the launcher in a Binder transaction, whose buffer is about 1 MB for
 * everything in flight. Kept to this many pixels (4 bytes each), the picture stays well inside it;
 * a widget bigger than that is drawn at a lower density and scaled up.
 */
private const val MAX_ART_PIXELS = 160_000f

/** Pixels per dp to draw a [widthDp] by [heightDp] picture at: the screen's, or less to stay in budget. */
internal fun artScale(
    widthDp: Float,
    heightDp: Float,
    density: Float,
): Float = min(density, sqrt(MAX_ART_PIXELS / (widthDp * heightDp).coerceAtLeast(1f)))
