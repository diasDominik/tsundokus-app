package uk.tsundokus.composeapp

import uk.tsundokus.features.orders.presentation.widgets.WidgetTimelines

/**
 * Called from Swift at launch with `WidgetCenter.shared.reloadAllTimelines()`: WidgetKit has no
 * Objective-C API, so Kotlin can't reach it, but it can call what Swift hands it.
 */
fun installWidgetReloader(reload: () -> Unit) {
    WidgetTimelines.reloader = reload
}
