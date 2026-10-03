import Foundation

/// Which sentence an arrival gets. The same rules as `ArrivalWording.kt`, which the Android widgets
/// use; the iOS widget runs without Kotlin, so they are repeated here.
enum ArrivalPhrase {
    /// Ordered, waiting for its release.
    case releases
    /// On the way and not due yet.
    case arrives
    /// Reported late, to a date still ahead.
    case delayedTo
    /// Its date has passed and it still hasn't arrived.
    case overdue
}

extension WidgetArrival {
    func phrase(on today: String) -> ArrivalPhrase {
        if isRelease { return .releases }
        if date < today { return .overdue }
        return status == "DELAYED" ? .delayedTo : .arrives
    }

    /// "Arrives today", "Releases tomorrow", "Delayed to Fri, 9 Oct", "Was due Thu, 1 Oct".
    func detail(on today: String) -> String {
        let day = dayName(date, today: today)
        switch phrase(on: today) {
        case .releases: return String(localized: "Releases \(day)")
        case .arrives: return String(localized: "Arrives \(day)")
        case .delayedTo: return String(localized: "Delayed to \(day)")
        case .overdue: return String(localized: "Was due \(day)")
        }
    }
}

/// "today", "tomorrow", or the day the way the device's language writes a short date.
func dayName(_ iso: String, today: String) -> String {
    if iso == today { return String(localized: "today") }
    if iso == IsoDay.adding(1, to: today) { return String(localized: "tomorrow") }
    guard let date = IsoDay.date(from: iso) else { return iso }
    return date.formatted(.dateTime.weekday(.abbreviated).day().month(.abbreviated))
}
