package uk.tsundokus.features.orders.presentation.widgets

/** Hands a snapshot to the platform's home-screen widgets and has them redraw. */
fun interface WidgetPublisher {
    suspend fun publish(snapshot: WidgetSnapshot)
}

/** Where the platform has no home-screen widgets. */
object NoWidgetPublisher : WidgetPublisher {
    override suspend fun publish(snapshot: WidgetSnapshot) = Unit
}
