package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File

/**
 * The snapshot both widgets draw. Kept in a file, so a widget drawn in a fresh process — after a
 * reboot, or on its periodic redraw — still has it, and in a flow, so a widget whose Glance session
 * is still open redraws with a new one at once: an open session only recomposes on update, it
 * doesn't run provideGlance again.
 *
 * The file is written to a temporary name and moved into place, so a reader gets the old snapshot
 * or the new one, never half of one.
 */
internal object WidgetSnapshotStore {
    private const val FILE_NAME = "widget_snapshot.json"

    @Volatile
    private var latest: MutableStateFlow<WidgetSnapshot>? = null

    /** The current snapshot and every later one written in this process; read from the file at first. */
    fun snapshots(context: Context): StateFlow<WidgetSnapshot> = flowFor(context)

    fun write(
        context: Context,
        snapshot: WidgetSnapshot,
    ) {
        val partial = File(context.filesDir, "$FILE_NAME.partial")
        partial.writeText(snapshot.encode())
        partial.renameTo(File(context.filesDir, FILE_NAME))
        flowFor(context).value = snapshot
    }

    private fun flowFor(context: Context): MutableStateFlow<WidgetSnapshot> =
        latest ?: synchronized(this) { latest ?: MutableStateFlow(readFile(context)).also { latest = it } }

    private fun readFile(context: Context): WidgetSnapshot {
        val file = File(context.filesDir, FILE_NAME)
        return WidgetSnapshot.decodeOrSignedOut(if (file.exists()) file.readText() else null)
    }
}
