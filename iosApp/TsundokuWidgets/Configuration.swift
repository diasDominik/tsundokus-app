import AppIntents
import SwiftUI
import WidgetKit

/// The colours a widget wears: the system's, or the app's own.
enum WidgetColours: String, AppEnum {
    case system
    case tsundoku

    static let typeDisplayRepresentation: TypeDisplayRepresentation = "Colours"
    static let caseDisplayRepresentations: [WidgetColours: DisplayRepresentation] = [
        .system: "System",
        .tsundoku: "Tsundoku",
    ]
}

/// Edit Widget for Next arrivals. The same choices as the Android settings screen, except the
/// background: iOS doesn't allow see-through home screen widgets, and its own tinted and clear
/// modes take that role.
struct NextArrivalsConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Next arrivals"
    static let description = IntentDescription("The orders arriving next, and when.")

    @Parameter(title: "Show heading", default: true)
    var showHeading: Bool

    @Parameter(title: "Show upcoming releases", default: true)
    var showReleases: Bool

    @Parameter(title: "Colours", default: .system)
    var colours: WidgetColours
}

/// Edit Widget for Your pile.
struct PileConfiguration: WidgetConfigurationIntent {
    static let title: LocalizedStringResource = "Your pile"
    static let description = IntentDescription("How many arrived volumes are waiting to be read.")

    @Parameter(title: "Show heading", default: true)
    var showHeading: Bool

    @Parameter(title: "Show reading details", default: true)
    var showDetail: Bool

    @Parameter(title: "Colours", default: .system)
    var colours: WidgetColours
}

/// The colours a widget draws with. In the tinted and clear modes the system recolours everything,
/// and the heading is marked accentable so it takes the tint.
struct Palette {
    let accent: Color
    let release: Color
    let late: Color
    let text: Color
    let secondaryText: Color
    let background: Color

    static func of(_ colours: WidgetColours) -> Palette {
        switch colours {
        case .system:
            Palette(
                accent: .accentColor,
                release: .secondary,
                late: .red,
                text: .primary,
                secondaryText: .secondary,
                background: Color(uiColor: .systemBackground)
            )
        case .tsundoku:
            Palette(
                accent: Color("TsundokuPrimary"),
                release: Color("TsundokuSecondary"),
                late: Color("TsundokuError"),
                text: Color("TsundokuOnSurface"),
                secondaryText: Color("TsundokuOnSurfaceVariant"),
                background: Color("TsundokuBackground")
            )
        }
    }

    func tint(for phrase: ArrivalPhrase) -> Color {
        switch phrase {
        case .releases: release
        case .arrives: accent
        case .delayedTo, .overdue: late
        }
    }
}
