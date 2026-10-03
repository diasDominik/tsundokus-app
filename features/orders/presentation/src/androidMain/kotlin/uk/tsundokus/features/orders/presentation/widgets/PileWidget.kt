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
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
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

/** The pile as the widget shows it. [count] is null when [message] stands in for it. */
internal data class PileUi(
    val heading: String,
    val count: Int?,
    val countLabel: String,
    /** "2 being read · oldest 30 days", or null when neither applies. */
    val detail: String?,
    val message: String?,
)

/**
 * The tsundoku: how many arrived volumes wait to be read, and, where the height allows, a heading
 * and how many are being read and how long the oldest has waited. Drawn for its exact size, since
 * launchers give a 2x1 cell very different heights. A tap opens the app.
 */
class PileWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        val snapshots = withContext(Dispatchers.IO) { WidgetSnapshotStore.snapshots(context) }
        val first = pileUi(snapshots.value, todayIso())
        provideContent {
            // Collected here, not read above: an open session redraws with each new snapshot.
            val snapshot by snapshots.collectAsState()
            val ui by produceState(first, snapshot) { value = pileUi(snapshot, todayIso()) }
            GlanceTheme { PileContent(ui) }
        }
    }

    internal companion object {
        /** Tall enough for the heading above the count. */
        val HEADING_FROM = 84.dp

        /** Tall enough for the heading, the count and the detail line under it. */
        val DETAIL_FROM = 104.dp
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
    )
}

@Composable
internal fun PileContent(ui: PileUi) {
    val context = LocalContext.current
    val height = LocalSize.current.height
    Column(
        modifier = GlanceModifier.widgetCard().clickable(actionStartActivity(openAppIntent(context))),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (height >= PileWidget.HEADING_FROM) WidgetHeading(ui.heading)
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
        if (ui.detail != null && height >= PileWidget.DETAIL_FROM) {
            Text(
                text = ui.detail,
                style = TextStyle(fontSize = 12.sp, color = GlanceTheme.colors.onSurfaceVariant),
                maxLines = 1,
                modifier = GlanceModifier.semantics { testTag = "detail" },
            )
        }
    }
}
