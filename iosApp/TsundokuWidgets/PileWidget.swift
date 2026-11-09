import SwiftUI
import WidgetKit

/// The tsundoku: how many arrived volumes wait to be read. On the home screen with how many are being
/// read and how long the oldest has waited, and the pile itself drawn gathering dust (see `PileArt`);
/// on the Lock Screen, the count. A tap opens the app.
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
        .supportedFamilies([.systemSmall, .systemMedium, .accessoryCircular, .accessoryRectangular, .accessoryInline])
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

    @ViewBuilder
    private var homeScreen: some View {
        if message == nil, !snapshot.pileBooks.isEmpty {
            withPile
        } else {
            countOnly
        }
    }

    /// The count made smaller, with the pile drawn in the room under it (see `PileArt`); in the
    /// medium size, beside it, where the pile gets the whole height.
    @ViewBuilder
    private var withPile: some View {
        if family == .systemMedium {
            HStack(spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    words(detailLines: 2)
                    Spacer(minLength: 0)
                }
                .frame(maxHeight: .infinity, alignment: .topLeading)
                // The words take what they need; the pile gets the rest of the width.
                .layoutPriority(1)
                pileArt
            }
        } else {
            VStack(alignment: .leading, spacing: 2) {
                words(detailLines: 1)
                pileArt.padding(.top, 4)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
    }

    private var pileArt: some View {
        PileArtView(books: snapshot.pileBooks, today: IsoDay.string(for: entry.date))
            .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    /// The heading, the count and the detail line, as they sit by the pile.
    @ViewBuilder
    private func words(detailLines: Int) -> some View {
        if entry.configuration.showHeading {
            Text("Your pile")
                .font(.caption.weight(.semibold))
                .foregroundStyle(palette.accent)
                .widgetAccentable()
        }
        HStack(alignment: .firstTextBaseline, spacing: 6) {
            Text("\(snapshot.unread)")
                .font(.system(size: 30, weight: .bold, design: .rounded))
                .foregroundStyle(palette.text)
            Text("to read").font(.footnote).foregroundStyle(palette.secondaryText)
        }
        .lineLimit(1)
        .minimumScaleFactor(0.7)
        if entry.configuration.showDetail, let detail {
            Text(detail).font(.caption2).foregroundStyle(palette.secondaryText).lineLimit(detailLines)
        }
    }

    private var countOnly: some View {
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

#Preview("Medium", as: .systemMedium) {
    PileWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: PileConfiguration())
}

#Preview("Lock Screen", as: .accessoryCircular) {
    PileWidget()
} timeline: {
    SnapshotEntry(date: .now, snapshot: .preview(on: IsoDay.string(for: .now)), configuration: PileConfiguration())
}
