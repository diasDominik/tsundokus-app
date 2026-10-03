import SwiftUI
import WidgetKit

/// Tsundoku's widgets. They read the snapshot the app writes into the shared App Group container and
/// never run the app's Kotlin code: a widget extension is small and starts on its own.
@main
struct TsundokuWidgetsBundle: WidgetBundle {
    var body: some Widget {
        NextArrivalsWidget()
        PileWidget()
    }
}
