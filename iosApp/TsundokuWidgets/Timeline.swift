import AppIntents
import WidgetKit

struct SnapshotEntry<Configuration: WidgetConfigurationIntent>: TimelineEntry {
    let date: Date
    let snapshot: WidgetSnapshot
    let configuration: Configuration
}

/// Both widgets' timeline: the snapshot as it is now, redrawn at each of the next few midnights so
/// "today" and "tomorrow" stay right and past releases drop off. The app has WidgetKit reload as soon
/// as the snapshot changes; after the last midnight, WidgetKit asks again.
struct SnapshotProvider<Configuration: WidgetConfigurationIntent>: AppIntentTimelineProvider {
    private static var midnights: Int { 3 }

    func placeholder(in context: Context) -> SnapshotEntry<Configuration> {
        SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: Configuration())
    }

    func snapshot(for configuration: Configuration, in context: Context) async -> SnapshotEntry<Configuration> {
        // The widget gallery shows made-up orders, so it never shows anyone's own titles.
        let snapshot = context.isPreview ? .preview(on: IsoDay.string(for: .now)) : WidgetSnapshot.read()
        return SnapshotEntry(date: .now, snapshot: snapshot, configuration: configuration)
    }

    func timeline(for configuration: Configuration, in context: Context) async -> Timeline<SnapshotEntry<Configuration>> {
        let snapshot = WidgetSnapshot.read()
        let midnights = IsoDay.midnights(after: .now, count: Self.midnights)
        let entries = ([Date.now] + midnights).map {
            SnapshotEntry(date: $0, snapshot: snapshot, configuration: configuration)
        }
        return Timeline(entries: entries, policy: .after(midnights.last ?? .now.addingTimeInterval(86_400)))
    }
}

extension WidgetSnapshot {
    /// Made-up orders for the widget gallery and previews, dated from `today`.
    static func preview(on today: String) -> WidgetSnapshot {
        WidgetSnapshot(
            signedIn: true,
            arrivals: [
                WidgetArrival(id: "preview-1", title: "Frieren", volume: "13", date: today, status: "SHIPPED"),
                WidgetArrival(id: "preview-2", title: "Dandadan", volume: "18", date: IsoDay.adding(1, to: today) ?? today, status: "ORDERED"),
                WidgetArrival(id: "preview-3", title: "Berserk", volume: "42", date: IsoDay.adding(6, to: today) ?? today, status: "DELAYED"),
                WidgetArrival(id: "preview-4", title: "Chainsaw Man", volume: "21", date: IsoDay.adding(11, to: today) ?? today, status: "ORDERED"),
            ],
            unread: 12,
            reading: 2,
            oldestUnreadSince: IsoDay.adding(-30, to: today)
        )
    }
}
