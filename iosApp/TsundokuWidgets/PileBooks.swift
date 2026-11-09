import Foundation

/// One unread volume in the pile widget's drawing.
///
/// Mirrors the Kotlin `WidgetPileBook` (features/orders/presentation, `PileBooks.kt`), and so does
/// how dusty a volume looks and what tells one volume from the next: the iOS widget runs without
/// Kotlin, so the rules are repeated here.
struct WidgetPileBook: Decodable {
    var id: String
    /// When it arrived (ISO `yyyy-MM-dd`); blank when that was never recorded.
    var since: String
    /// Being read: pulled half out of the pile, bookmark in, no dust. Optional so a snapshot that
    /// leaves it out still reads.
    var reading: Bool?

    var isReading: Bool { reading ?? false }
}

/// How neglected a volume in the pile looks, from how long it has waited.
enum DustStage: Int, Comparable {
    /// Under two weeks: fresh.
    case clean
    /// Two weeks: a film of dust on top.
    case light
    /// A month: thick dust, the colour going grey.
    case thick
    /// Two months: cobwebs between it and the next one.
    case cobweb
    /// Four months: bleached, sagging, and a spider has moved in.
    case spider

    static func < (lhs: DustStage, rhs: DustStage) -> Bool { lhs.rawValue < rhs.rawValue }
}

extension WidgetPileBook {
    static let lightDustFromDays = 14
    static let thickDustFromDays = 30
    static let cobwebFromDays = 60
    static let spiderFromDays = 120

    /// How this volume looks on `today`. Worked out while drawing, not stored, so the dust keeps
    /// settling while the app stays closed. One being read was just picked up: no dust. One with no
    /// arrival date is left clean rather than guessed at.
    func dust(on today: String) -> DustStage {
        if isReading { return .clean }
        guard let days = IsoDay.days(from: since, to: today) else { return .clean }
        switch days {
        case Self.spiderFromDays...: return .spider
        case Self.cobwebFromDays...: return .cobweb
        case Self.thickDustFromDays...: return .thick
        case Self.lightDustFromDays...: return .light
        default: return .clean
        }
    }

    /// What tells this volume from the next, picked from its id the same way as on Android, so a
    /// book looks the same every time the widget redraws.
    func look(colours: Int) -> PileBookLook {
        let hash = fnv1a(id)

        func part(_ shiftBits: UInt64, _ steps: Int) -> Int { Int((hash >> shiftBits) & 0xFFFF) % steps }
        return PileBookLook(
            colour: part(0, colours),
            length: 0.72 + CGFloat(part(8, 25)) / 100,
            shift: CGFloat(part(16, 17) - 8) / 100,
            thickness: 0.8 + CGFloat(part(24, 41)) / 100,
            pagesOut: part(32, 5) == 0,
            seed: hash
        )
    }
}

/// Fractions are of the pile's width or of a spine's thickness.
struct PileBookLook {
    /// Index into the drawing's spine colours.
    let colour: Int
    /// How long the spine is, of the pile's width.
    let length: CGFloat
    /// How far its middle sits from the pile's middle, of the pile's width; negative is left.
    let shift: CGFloat
    /// Thicker or thinner than the average volume.
    let thickness: CGFloat
    /// Lies the other way round: its page edges face out instead of its spine.
    let pagesOut: Bool
    /// Seed for the dust grains, so they don't move between redraws.
    let seed: UInt64
}

/// FNV-1a, 64 bit, over UTF-16 code units: the same hash the Kotlin side takes of a `String`.
func fnv1a(_ text: String) -> UInt64 {
    var hash: UInt64 = 0xCBF2_9CE4_8422_2325
    for unit in text.utf16 {
        hash ^= UInt64(unit)
        hash = hash &* 0x0000_0100_0000_01B3
    }
    return hash
}

/// SplitMix64: a small seeded generator, so dust grains and fluff land in the same place on every
/// redraw. Its numbers differ from Kotlin's `Random`; only the books' looks have to match Android.
struct SeededRandom {
    private var state: UInt64

    init(seed: UInt64) { state = seed }

    mutating func next() -> UInt64 {
        state = state &+ 0x9E37_79B9_7F4A_7C15
        var z = state
        z = (z ^ (z >> 30)) &* 0xBF58_476D_1CE4_E5B9
        z = (z ^ (z >> 27)) &* 0x94D0_49BB_1331_11EB
        return z ^ (z >> 31)
    }

    /// In `0..<1`.
    mutating func nextFloat() -> CGFloat { CGFloat(next() >> 40) / CGFloat(1 << 24) }

    /// In `0..<bound`.
    mutating func nextInt(_ bound: Int) -> Int { Int(next() % UInt64(bound)) }

    mutating func nextBool() -> Bool { next() & 1 == 0 }
}
