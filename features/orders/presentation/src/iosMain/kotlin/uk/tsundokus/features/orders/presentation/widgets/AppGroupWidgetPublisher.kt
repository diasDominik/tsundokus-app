package uk.tsundokus.features.orders.presentation.widgets

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSBundle
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.writeToURL

/** The Info.plist key both the app and its widget extension read the App Group identifier from. */
private const val APP_GROUP_KEY = "TsundokuAppGroup"

/** The file in the App Group container that the widget extension reads; see TsundokuWidgets. */
private const val SNAPSHOT_FILE = "widget_snapshot.json"

/**
 * Writes the snapshot into the App Group container the WidgetKit extension shares with the app, then
 * has WidgetKit reload. The extension is Swift and runs on its own, so a file is all it gets.
 */
class AppGroupWidgetPublisher : WidgetPublisher {
    @OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
    override suspend fun publish(snapshot: WidgetSnapshot) {
        val group = NSBundle.mainBundle.objectForInfoDictionaryKey(APP_GROUP_KEY) as? String ?: return
        val container =
            NSFileManager.defaultManager.containerURLForSecurityApplicationGroupIdentifier(group) ?: return
        val file = container.URLByAppendingPathComponent(SNAPSHOT_FILE) ?: return
        // Atomically: written to a temporary file and moved into place, so the widget never reads half.
        NSString
            .create(
                string = snapshot.encode(),
            ).writeToURL(file, atomically = true, encoding = NSUTF8StringEncoding, error = null)
        WidgetTimelines.reload()
    }
}

/**
 * Asks WidgetKit to redraw. WidgetCenter is Swift-only, so the app hands the call in at launch
 * (installWidgetReloader in composeApp); until it has, there is nothing to reload.
 */
object WidgetTimelines {
    var reloader: (() -> Unit)? = null

    fun reload() {
        reloader?.invoke()
    }
}
