package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Writes the snapshot where the widgets read it and has every placed widget redraw. */
class GlanceWidgetPublisher(
    private val context: Context,
) : WidgetPublisher {
    override suspend fun publish(snapshot: WidgetSnapshot) {
        withContext(Dispatchers.IO) { WidgetSnapshotStore.write(context, snapshot) }
        NextArrivalsWidget().updateAll(context)
        PileWidget().updateAll(context)
        WidgetPreviews.publishOnce(context)
    }
}
