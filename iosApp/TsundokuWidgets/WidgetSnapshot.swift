import Foundation

/// What the widgets show, as the app wrote it into the App Group container.
///
/// Mirrors the Kotlin `WidgetSnapshot` (features/orders/presentation). The JSON is a contract:
/// `WidgetSnapshotContractTest` pins its shape on the Kotlin side, so a change there means a change
/// here. Dates are ISO `yyyy-MM-dd` calendar days.
struct WidgetSnapshot: Decodable {
    var signedIn: Bool
    var arrivals: [WidgetArrival]
    var unread: Int
    var reading: Int
    var oldestUnreadSince: String?

    static let signedOut = WidgetSnapshot(signedIn: false, arrivals: [], unread: 0, reading: 0, oldestUnreadSince: nil)

    /// The snapshot the app last wrote; signed out when there is none or it can't be read.
    static func read() -> WidgetSnapshot {
        guard
            let group = Bundle.main.object(forInfoDictionaryKey: "TsundokuAppGroup") as? String,
            let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: group),
            let data = try? Data(contentsOf: container.appendingPathComponent("widget_snapshot.json")),
            let snapshot = try? JSONDecoder().decode(WidgetSnapshot.self, from: data)
        else { return .signedOut }
        return snapshot
    }

    /// The arrivals still ahead on `today`: an order whose release date has passed drops off.
    func arrivals(on today: String) -> [WidgetArrival] {
        arrivals.filter { !$0.isRelease || $0.date >= today }
    }

    /// How long the oldest unread volume has waited by `today`.
    func oldestWaitingDays(on today: String) -> Int? {
        oldestUnreadSince.flatMap { IsoDay.days(from: $0, to: today) }.map { max($0, 0) }
    }
}

struct WidgetArrival: Decodable, Identifiable {
    var id: String
    var title: String
    var volume: String
    /// The day it is expected, released or delayed to.
    var date: String
    /// ORDERED (a release), SHIPPED or DELAYED.
    var status: String

    var isRelease: Bool { status == "ORDERED" }

    var label: String { [title, volume].filter { !$0.isEmpty }.joined(separator: " ") }

    /// The link the app routes like any other: it opens this order.
    var url: URL? { URL(string: "tsundokus://tsundokus.uk/orders/\(id)") }
}

/// ISO `yyyy-MM-dd` days in the device's calendar and time zone.
enum IsoDay {
    private static let calendar = Calendar.current

    static func string(for date: Date) -> String {
        let parts = calendar.dateComponents([.year, .month, .day], from: date)
        return String(format: "%04d-%02d-%02d", parts.year ?? 0, parts.month ?? 0, parts.day ?? 0)
    }

    static func date(from iso: String) -> Date? {
        let parts = iso.split(separator: "-").compactMap { Int($0) }
        guard parts.count == 3 else { return nil }
        return calendar.date(from: DateComponents(year: parts[0], month: parts[1], day: parts[2]))
    }

    static func adding(_ days: Int, to iso: String) -> String? {
        date(from: iso).flatMap { calendar.date(byAdding: .day, value: days, to: $0) }.map(string(for:))
    }

    static func days(from start: String, to end: String) -> Int? {
        guard let from = date(from: start), let to = date(from: end) else { return nil }
        return calendar.dateComponents([.day], from: from, to: to).day
    }

    /// The next few midnights after `date`: when "today" and "tomorrow" have to be redrawn.
    static func midnights(after date: Date, count: Int) -> [Date] {
        let first = calendar.startOfDay(for: date)
        return (1...count).compactMap { calendar.date(byAdding: .day, value: $0, to: first) }
    }
}
