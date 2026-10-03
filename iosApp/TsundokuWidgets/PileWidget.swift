import SwiftUI
import WidgetKit

/// The tsundoku: how many arrived volumes wait to be read. On the home screen with how many are being
/// read and how long the oldest has waited; on the Lock Screen, the count. A tap opens the app.
struct PileWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(
            kind: "Pile",
            intent: PileConfiguration.self,
            provider: SnapshotProvider<PileConfiguration>()
        ) { entry in
            PileView(entry: entry)
                .containerBackground(Palette.of(entry.configuration.colours).background, for: .widget)
        }
        .configurationDisplayName("Your pile")
        .description("How many arrived volumes are waiting to be read.")
        .supportedFamilies([.systemSmall, .accessoryCircular, .accessoryRectangular, .accessoryInline])
    }
}

struct PileView: View {
    @Environment(\.widgetFamily) private var family
    let entry: SnapshotEntry<PileConfiguration>

    private var snapshot: WidgetSnapshot { entry.snapshot }
    private var palette: Palette { .of(entry.configuration.colours) }

    /// What stands in for the count: signed out, or nothing left to read.
    private var message: String? {
        if !snapshot.signedIn { return String(localized: "Sign in to Tsundoku") }
        return snapshot.unread == 0 ? String(localized: "All caught up") : nil
    }

    /// "2 being read · oldest 30 days", or nil when neither applies.
    private var detail: String? {
        let today = IsoDay.string(for: entry.date)
        let parts = [
            snapshot.reading > 0 ? String(localized: "\(snapshot.reading) being read") : nil,
            snapshot.oldestWaitingDays(on: today).map { String(localized: "oldest \($0) days") },
        ].compactMap { $0 }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }

    var body: some View {
        switch family {
        case .accessoryInline:
            Label(message ?? String(localized: "\(snapshot.unread) to read"), systemImage: "books.vertical")
        case .accessoryCircular:
            VStack(spacing: 0) {
                if message == nil {
                    Text("\(snapshot.unread)").font(.title2.weight(.bold)).widgetAccentable()
                    Text("to read").font(.caption2)
                } else {
                    Image(systemName: snapshot.signedIn ? "checkmark" : "books.vertical").font(.title3)
                }
            }
        case .accessoryRectangular:
            VStack(alignment: .leading, spacing: 1) {
                Text("Your pile").font(.headline).widgetAccentable()
                Text(message ?? String(localized: "\(snapshot.unread) to read")).font(.caption)
                if message == nil, entry.configuration.showDetail, let detail {
                    Text(detail).font(.caption2).lineLimit(1)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        default:
            homeScreen
        }
    }

    private var homeScreen: some View {
        VStack(alignment: .leading, spacing: 4) {
            if entry.configuration.showHeading {
                Text("Your pile")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(palette.accent)
                    .widgetAccentable()
            }
            Spacer(minLength: 0)
            if let message {
                Text(message).font(.subheadline).foregroundStyle(palette.secondaryText)
            } else {
                Text("\(snapshot.unread)")
                    .font(.system(size: 44, weight: .bold, design: .rounded))
                    .foregroundStyle(palette.text)
                    .minimumScaleFactor(0.6)
                    .lineLimit(1)
                Text("to read").font(.subheadline).foregroundStyle(palette.secondaryText)
                if entry.configuration.showDetail, let detail {
                    Text(detail).font(.caption2).foregroundStyle(palette.secondaryText).lineLimit(2)
                }
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
    }
}

#Preview("Small", as: .systemSmall) {
    PileWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: PileConfiguration())
}

#Preview("Lock Screen", as: .accessoryCircular) {
    PileWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: PileConfiguration())
}
