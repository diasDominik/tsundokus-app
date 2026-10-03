import SwiftUI
import WidgetKit

/// The orders arriving next, soonest first. WidgetKit widgets don't scroll, so each size shows what
/// fits: two small or medium, seven large (one more each without the heading); on the Lock Screen,
/// the next one or two. A tap on an order opens it in medium and large; a small widget has a single
/// tap target, so it opens the app, and the Lock Screen ones open the next arrival.
struct NextArrivalsWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: "NextArrivals",
            intent: NextArrivalsConfiguration.self,
            provider: SnapshotProvider<NextArrivalsConfiguration>()
        ) { entry in
            NextArrivalsView(entry: entry)
                .containerBackground(Palette.of(entry.configuration.colours).background, for: .widget)
        }
        .configurationDisplayName("Next arrivals")
        .description("The orders arriving next, and when.")
        .supportedFamilies([.systemSmall, .systemMedium, .systemLarge, .accessoryRectangular, .accessoryInline])
    }
}

struct NextArrivalsView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SnapshotEntry<NextArrivalsConfiguration>

    private var today: String { IsoDay.string(for: entry.date) }
    private var palette: Palette { .of(entry.configuration.colours) }

    private var arrivals: [WidgetArrival] {
        let ahead = entry.snapshot.arrivals(on: today)
        return entry.configuration.showReleases ? ahead : ahead.filter { !$0.isRelease }
    }

    /// What stands in for the list when there is nothing to list.
    private var message: String? {
        if !entry.snapshot.signedIn { return String(localized: "Sign in to Tsundoku") }
        return arrivals.isEmpty ? String(localized: "Nothing on the way") : nil
    }

    private var room: Int {
        let headingRow = entry.configuration.showHeading ? 0 : 1
        switch family {
        case .accessoryInline: return 1
        case .accessoryRectangular: return 2
        case .systemSmall, .systemMedium: return 2 + headingRow
        default: return 7 + headingRow
        }
    }

    var body: some View {
        switch family {
        case .accessoryInline:
            if let first = arrivals.first, message == nil {
                Text("\(first.label) · \(dayName(first.date, today: today))")
                    .widgetURL(first.url)
            } else {
                Text(message ?? "")
            }
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                if let message {
                    Text(message).font(.caption)
                } else {
                    ForEach(arrivals.prefix(room)) { arrival in
                        Text(arrival.label).font(.headline).lineLimit(1).widgetAccentable()
                        Text(arrival.detail(on: today)).font(.caption).lineLimit(1)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .widgetURL(arrivals.first?.url)
        default:
            homeScreen
        }
    }

    private var homeScreen: some View {
        VStack(alignment: .leading, spacing: 6) {
            if entry.configuration.showHeading {
                Text("Next arrivals")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(palette.accent)
                    .widgetAccentable()
            }
            if let message {
                Text(message).font(.subheadline).foregroundStyle(palette.secondaryText)
            } else if family == .systemSmall {
                // A small widget takes one tap target, and the whole widget is it: it opens the app.
                ForEach(arrivals.prefix(room)) { row($0, titleLines: 1) }
            } else {
                ForEach(arrivals.prefix(room)) { arrival in
                    Link(destination: arrival.url ?? URL(string: "tsundokus://tsundokus.uk")!) {
                        row(arrival, titleLines: 1)
                    }
                }
            }
            Spacer(minLength: 0)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }

    private func row(_ arrival: WidgetArrival, titleLines: Int) -> some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(arrival.label)
                .font(.subheadline.weight(.bold))
                .foregroundStyle(palette.text)
                .lineLimit(titleLines)
            Text(arrival.detail(on: today))
                .font(.caption)
                .foregroundStyle(palette.tint(for: arrival.phrase(on: today)))
                .lineLimit(1)
        }
    }
}

#Preview("Small", as: .systemSmall) {
    NextArrivalsWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: NextArrivalsConfiguration())
}

#Preview("Large", as: .systemLarge) {
    NextArrivalsWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: NextArrivalsConfiguration())
}

#Preview("Lock Screen", as: .accessoryRectangular) {
    NextArrivalsWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: NextArrivalsConfiguration())
}
