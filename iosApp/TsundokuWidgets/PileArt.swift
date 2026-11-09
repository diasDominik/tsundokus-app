import SwiftUI

/// The pile widget's picture: every unread volume as a book lying in a stack, the longest-waiting at
/// the bottom, gathering dust, cobwebs and finally a spider the longer it waits.
///
/// A port of the Android `PileArt.kt`, in points where that one is in dp. A pile taller than its
/// space runs out of the top. Its colours don't follow the theme — books have their own — and the
/// cobwebs and the spider carry both a light and a dark edge, so the one picture reads on a light
/// background, a dark one and the tinted and clear modes alike.
struct PileArtView: View {
    let books: [WidgetPileBook]
    let today: String

    var body: some View {
        Canvas { context, size in
            PileArt.draw(in: &context, size: size, books: books, today: today)
        }
        // The words above it say what it shows.
        .accessibilityHidden(true)
    }
}

enum PileArt {
    /// Thickest a volume is drawn, when the pile is short.
    private static let maxThickness: CGFloat = 13
    /// Thinnest: a pile taller than the space at this thickness runs out of the top.
    private static let minThickness: CGFloat = 3
    /// The pile never gets wider than this, so a wide widget doesn't get absurdly long books.
    private static let maxPileWidth: CGFloat = 140
    /// Of the space's width the pile takes, at the right; dust bunnies gather on the left.
    private static let pileWidthFraction: CGFloat = 0.72
    /// How far a volume being read is pulled out of the pile, of the pile's width.
    private static let pulledOut: CGFloat = 0.2

    private static let spines: [Rgb] = [
        Rgb(0x8E2C2C), // oxblood
        Rgb(0x1F3A5F), // navy
        Rgb(0x2E5E4E), // forest
        Rgb(0xC9A227), // mustard
        Rgb(0x2A7F86), // teal
        Rgb(0x5B3A6B), // plum
        Rgb(0xC4622D), // burnt orange
        Rgb(0x4A5560), // slate
        Rgb(0xE9E3D5), // manga white
        Rgb(0x7A2E4A), // wine
        Rgb(0x3F6E9E), // blue
        Rgb(0x6B7F3A), // olive
    ]

    private static let dust = Rgb(0x857E70)
    private static let dustLight = Rgb(0xA9A291)
    private static let pages = Rgb(0xEDE6D3)
    private static let ribbon = Rgb(0xC0392B)
    private static let thread = Rgb(0xF4F4F4)
    private static let spiderBody = Rgb(0x151515)
    private static let white = Rgb(0xFFFFFF)
    private static let black = Rgb(0x000000)

    /// Where the last web on each side of the pile reaches up to, so the next doesn't overlap it.
    private struct Webs {
        var leftTop = CGFloat.greatestFiniteMagnitude
        var rightTop = CGFloat.greatestFiniteMagnitude
    }

    /// One volume where it ends up in the picture.
    private struct Placed {
        let book: WidgetPileBook
        let look: PileBookLook
        let stage: DustStage
        let left: CGFloat
        let right: CGFloat
        let top: CGFloat
        let bottom: CGFloat

        var thickness: CGFloat { bottom - top }
        var length: CGFloat { right - left }
    }

    static func draw(in context: inout GraphicsContext, size: CGSize, books: [WidgetPileBook], today: String) {
        guard !books.isEmpty, size.width > 0, size.height > 0 else { return }
        context.clip(to: Path(CGRect(origin: .zero, size: size)))
        let placed = place(size: size, books: books, today: today)
        drawDustBunnies(&context, placed)
        // Bottom first: each volume lies on, and covers part of, the one below.
        var webs = Webs()
        for (index, book) in placed.enumerated() {
            drawBook(&context, book)
            if index > 0 { drawWeb(&context, lower: placed[index - 1], upper: book, webs: &webs) }
        }
        if let home = placed.lastIndex(where: { $0.stage == .spider }) {
            drawSpider(&context, home: placed[home], above: placed[safe: home + 1], floor: size.height)
        }
    }

    private static func place(size: CGSize, books: [WidgetPileBook], today: String) -> [Placed] {
        let pileWidth = min(size.width * pileWidthFraction, maxPileWidth)
        // Room on the right for the longest volume shifted furthest that way.
        let middle = size.width - pileWidth * 0.58 - 2
        let looks = books.map { $0.look(colours: spines.count) }
        let fitting = size.height * 0.94 / looks.reduce(0) { $0 + $1.thickness }
        let unit = min(max(fitting, minThickness), maxThickness)
        var bottom = size.height - 1
        var placed: [Placed] = []
        for (book, look) in zip(books, looks) {
            // Out of the top of its space: the rest of the pile is not drawn.
            if bottom <= 0 { break }
            let top = bottom - unit * look.thickness
            let length = pileWidth * look.length
            let centre = middle + pileWidth * look.shift - (book.isReading ? pileWidth * pulledOut : 0)
            placed.append(
                Placed(
                    book: book,
                    look: look,
                    stage: book.dust(on: today),
                    left: centre - length / 2,
                    right: centre + length / 2,
                    top: top,
                    bottom: bottom
                )
            )
            bottom = top
        }
        return placed
    }

    // MARK: - Books

    private static func drawBook(_ context: inout GraphicsContext, _ book: Placed) {
        let sag = book.stage == .spider ? min(book.thickness * 0.22, 2) : 0
        let radius = min(book.thickness * 0.25, 2)
        let outline = bookOutline(book, radius: radius, sag: sag)

        // Shadow on the book underneath.
        var shadow = context
        shadow.addFilter(.blur(radius: 1.4))
        shadow.fill(outline.offsetBy(dx: 0, dy: 0.8), with: .color(black.color(alpha: 90)))

        let base = faded(spines[book.look.colour], book.stage)
        var inside = context
        inside.clip(to: outline)
        if book.look.pagesOut {
            drawPageEdge(&inside, book, base: base)
        } else {
            drawSpine(&inside, book, base: base)
        }
        // Light catching the rounded top of the spine.
        inside.fill(
            Path(CGRect(x: book.left, y: book.top + book.thickness * 0.12, width: book.length, height: book.thickness * 0.12)),
            with: .color(white.color(alpha: 45))
        )
        drawDust(&inside, book)

        if book.book.isReading { drawRibbon(&context, book) }
    }

    /// A rounded rectangle, bowed down in the middle when `sag` says it has started to give way.
    private static func bookOutline(_ book: Placed, radius: CGFloat, sag: CGFloat) -> Path {
        let rect = CGRect(x: book.left, y: book.top, width: book.length, height: book.thickness)
        if sag == 0 { return Path(roundedRect: rect, cornerRadius: radius) }
        let middle = (book.left + book.right) / 2
        var path = Path()
        path.move(to: CGPoint(x: book.left + radius, y: book.top))
        path.addQuadCurve(to: CGPoint(x: book.right - radius, y: book.top), control: CGPoint(x: middle, y: book.top + sag * 2))
        path.addQuadCurve(to: CGPoint(x: book.right, y: book.top + radius), control: CGPoint(x: book.right, y: book.top))
        path.addLine(to: CGPoint(x: book.right, y: book.bottom - radius))
        path.addQuadCurve(to: CGPoint(x: book.right - radius, y: book.bottom), control: CGPoint(x: book.right, y: book.bottom))
        path.addQuadCurve(to: CGPoint(x: book.left + radius, y: book.bottom), control: CGPoint(x: middle, y: book.bottom + sag * 2))
        path.addQuadCurve(to: CGPoint(x: book.left, y: book.bottom - radius), control: CGPoint(x: book.left, y: book.bottom))
        path.addLine(to: CGPoint(x: book.left, y: book.top + radius))
        path.addQuadCurve(to: CGPoint(x: book.left + radius, y: book.top), control: CGPoint(x: book.left, y: book.top))
        path.closeSubpath()
        return path
    }

    private static func drawSpine(_ context: inout GraphicsContext, _ book: Placed, base: Rgb) {
        context.fill(
            Path(CGRect(x: book.left, y: book.top - 3, width: book.length, height: book.thickness + 6)),
            with: roundness(book, base)
        )
        // Bands near each end, and a label in the middle on a spine thick enough to show one.
        let band = base.mixed(with: white, 0.45).color(alpha: 0x99)
        for at in [0.09, 0.91] as [CGFloat] {
            let x = book.left + book.length * at
            context.fill(Path(CGRect(x: x - 0.6, y: book.top, width: 1.2, height: book.thickness)), with: .color(band))
        }
        if book.thickness >= 6 {
            let labelHeight = book.thickness * 0.42
            let labelMiddle = (book.top + book.bottom) / 2
            let label = CGRect(
                x: book.left + book.length * 0.32,
                y: labelMiddle - labelHeight / 2,
                width: book.length * 0.36,
                height: labelHeight
            )
            context.fill(
                Path(roundedRect: label, cornerRadius: 1),
                with: .color(base.mixed(with: white, 0.55).color(alpha: 0x88))
            )
        }
    }

    /// A volume lying the other way: the cream block of its pages between the two covers.
    private static func drawPageEdge(_ context: inout GraphicsContext, _ book: Placed, base: Rgb) {
        let paper = faded(pages, book.stage)
        context.fill(
            Path(CGRect(x: book.left, y: book.top - 3, width: book.length, height: book.thickness + 6)),
            with: roundness(book, paper)
        )
        var lines = Path()
        var y = book.top + 1.2
        while y < book.bottom - 1 {
            lines.move(to: CGPoint(x: book.left, y: y))
            lines.addLine(to: CGPoint(x: book.right, y: y))
            y += 1.1
        }
        context.stroke(lines, with: .color(paper.mixed(with: black, 0.18).color()), lineWidth: 0.4)
        let cover = book.thickness * 0.13
        let boards = GraphicsContext.Shading.color(base.mixed(with: black, 0.15).color())
        context.fill(Path(CGRect(x: book.left, y: book.top - 1, width: book.length, height: cover + 1)), with: boards)
        context.fill(Path(CGRect(x: book.left, y: book.bottom - cover, width: book.length, height: cover + 1)), with: boards)
    }

    /// Lighter at the top, darker underneath: a spine is rounded, not flat.
    private static func roundness(_ book: Placed, _ colour: Rgb) -> GraphicsContext.Shading {
        .linearGradient(
            Gradient(stops: [
                .init(color: colour.mixed(with: white, 0.2).color(), location: 0),
                .init(color: colour.color(), location: 0.45),
                .init(color: colour.mixed(with: black, 0.32).color(), location: 1),
            ]),
            startPoint: CGPoint(x: 0, y: book.top),
            endPoint: CGPoint(x: 0, y: book.bottom)
        )
    }

    private static func drawDust(_ context: inout GraphicsContext, _ book: Placed) {
        let veil: Int, lip: CGFloat, grains: CGFloat
        switch book.stage {
        case .clean: return
        case .light: (veil, lip, grains) = (0, 0.7, 0.6)
        case .thick: (veil, lip, grains) = (45, 1.2, 1.6)
        case .cobweb: (veil, lip, grains) = (75, 1.6, 2.4)
        case .spider: (veil, lip, grains) = (105, 2, 3.2)
        }
        if veil > 0 {
            context.fill(
                Path(CGRect(x: book.left, y: book.top, width: book.length, height: book.thickness)),
                with: .color(dust.color(alpha: veil))
            )
        }
        var random = SeededRandom(seed: book.look.seed)
        // A soft, uneven layer settled along the top edge; never more than a slice of a thin volume.
        let depth = min(lip, book.thickness * 0.3)
        var layer = Path()
        layer.move(to: CGPoint(x: book.left, y: book.top))
        var x = book.left
        layer.addLine(to: CGPoint(x: x, y: book.top + depth * 0.6))
        while x < book.right {
            let nextX = min(x + 2 + random.nextFloat() * 3, book.right)
            let nextY = book.top + depth * (0.45 + random.nextFloat() * 0.55)
            let control = CGPoint(x: (x + nextX) / 2, y: book.top + depth * (0.7 + random.nextFloat() * 0.6))
            layer.addQuadCurve(to: CGPoint(x: nextX, y: nextY), control: control)
            x = nextX
        }
        layer.addLine(to: CGPoint(x: book.right, y: book.top))
        layer.closeSubpath()
        var soft = context
        soft.addFilter(.blur(radius: 0.35))
        soft.fill(layer, with: .color(dustLight.color(alpha: 0xC8)))

        // Grime: specks on the top edge at first, all over once it has been there a while.
        let count = Int(book.length * grains)
        let reach: CGFloat = book.stage == .light ? 0.35 : 1
        for _ in 0..<count {
            let x = book.left + random.nextFloat() * book.length
            let y = book.top + book.thickness * reach * random.nextFloat() * random.nextFloat()
            let colour = random.nextInt(4) == 0 ? dustLight : dust
            let alpha = 70 + random.nextInt(120)
            let radius = 0.18 + random.nextFloat() * 0.32
            context.fill(circle(x, y, radius), with: .color(colour.color(alpha: alpha)))
        }
    }

    /// The bookmark hanging out of a volume being read, notched at the end.
    private static func drawRibbon(_ context: inout GraphicsContext, _ book: Placed) {
        let x = book.left + book.length * 0.16
        let width: CGFloat = 2.4
        let end = book.bottom + book.thickness * 0.9 + 4
        var path = Path()
        path.move(to: CGPoint(x: x, y: book.bottom - 1))
        path.addLine(to: CGPoint(x: x + width, y: book.bottom - 1))
        path.addLine(to: CGPoint(x: x + width, y: end))
        path.addLine(to: CGPoint(x: x + width / 2, y: end - width * 0.8))
        path.addLine(to: CGPoint(x: x, y: end))
        path.closeSubpath()
        context.fill(path.offsetBy(dx: 0.5, dy: 0.6), with: .color(black.color(alpha: 70)))
        context.fill(path, with: .color(ribbon.color()))
    }

    // MARK: - Cobwebs and the spider

    /// A cobweb in the corner where `upper` lies on `lower`, on the side where `lower` sticks out
    /// more. Only once `lower` has waited long enough for spiders to bother.
    private static func drawWeb(_ context: inout GraphicsContext, lower: Placed, upper: Placed, webs: inout Webs) {
        if lower.stage < .cobweb { return }
        let leftOverhang = upper.left - lower.left
        let rightOverhang = lower.right - upper.right
        let onLeft = leftOverhang >= rightOverhang
        let overhang = max(leftOverhang, rightOverhang)
        let largest = max(min(upper.thickness * 2.6, 18), 8)
        let size = min(max(overhang * 1.5, 8), largest)
        // A thin pile has a corner every few points; one web per stretch of it is plenty.
        let lastTop = onLeft ? webs.leftTop : webs.rightTop
        if lower.top > lastTop - 2 { return }
        if onLeft { webs.leftTop = lower.top - size } else { webs.rightTop = lower.top - size }
        drawCornerWeb(
            &context,
            corner: CGPoint(x: onLeft ? upper.left : upper.right, y: lower.top),
            size: size,
            towardsLeft: onLeft,
            seed: lower.look.seed
        )
    }

    /// Threads fanned out from a corner, with sagging rings between them.
    private static func drawCornerWeb(
        _ context: inout GraphicsContext,
        corner: CGPoint,
        size: CGFloat,
        towardsLeft: Bool,
        seed: UInt64
    ) {
        var random = SeededRandom(seed: seed ^ 0x5EB)
        let spokes = 5
        let direction: CGFloat = towardsLeft ? -1 : 1
        // From along the lower book's top (0°) up to along the upper book's side (90°).
        let ends = (0..<spokes).map { spoke -> CGPoint in
            let angle = CGFloat(spoke) / CGFloat(spokes - 1) * .pi / 2
            let reach = size * (0.85 + random.nextFloat() * 0.25)
            return CGPoint(x: corner.x + direction * cos(angle) * reach, y: corner.y - sin(angle) * reach)
        }
        var web = Path()
        for end in ends {
            web.move(to: corner)
            web.addLine(to: end)
        }
        for ring in [0.35, 0.62, 0.9] as [CGFloat] {
            for index in 0..<(ends.count - 1) {
                let from = CGPoint(x: corner.x + (ends[index].x - corner.x) * ring, y: corner.y + (ends[index].y - corner.y) * ring)
                let to = CGPoint(x: corner.x + (ends[index + 1].x - corner.x) * ring, y: corner.y + (ends[index + 1].y - corner.y) * ring)
                // Sag towards the corner, a little different on every ring.
                let pull = 0.22 + CGFloat((index + Int(ring * 10)) % 3) * 0.05
                let midX = (from.x + to.x) / 2
                let midY = (from.y + to.y) / 2
                let control = CGPoint(x: midX + (corner.x - midX) * pull, y: midY + (corner.y - midY) * pull)
                if index == 0 { web.move(to: from) }
                web.addQuadCurve(to: to, control: control)
            }
        }
        // A dark hairline under the light thread, so the web shows on any background.
        context.stroke(web.offsetBy(dx: 0.35, dy: 0.35), with: .color(black.color(alpha: 90)), lineWidth: 0.55)
        context.stroke(web, with: .color(thread.color(alpha: 0xEE)), lineWidth: 0.55)
    }

    /// The spider: on its web on the highest volume that has waited long enough for one, or, with no
    /// web to sit in, dangling on a thread off that volume's end.
    private static func drawSpider(_ context: inout GraphicsContext, home: Placed, above: Placed?, floor: CGFloat) {
        let size: CGFloat = 3
        let x: CGFloat, y: CGFloat
        if let above {
            // Hanging off the end that sticks out more, on a thread from under the volume above.
            let onLeft = above.left - home.left >= home.right - above.right
            let threadX = onLeft ? home.left - size * 1.5 : home.right + size * 1.5
            // Down past the volume, but not through the floor.
            let bottom = min(home.bottom + size * 3.5, floor - size * 1.5)
            var line = Path()
            line.move(to: CGPoint(x: threadX, y: home.top - size))
            line.addLine(to: CGPoint(x: threadX, y: bottom - size * 1.2))
            context.stroke(line, with: .color(thread.color(alpha: 0xCC)), lineWidth: 0.4)
            (x, y) = (threadX, bottom)
        } else {
            // Top of the pile: walking across the top of it.
            (x, y) = (home.left + home.length * 0.7, home.top - size * 1.1)
        }
        // Four legs a side from the front body: the front pair reaching forward, the back pair back,
        // each bent up at the knee and down to the foot.
        var legs = Path()
        for side in [-1, 1] as [CGFloat] {
            for degrees in [-62, -22, 18, 52] as [CGFloat] {
                let angle = degrees * .pi / 180
                let outX = cos(angle) * side
                let outY = sin(angle)
                let start = CGPoint(x: x + outX * size * 0.4, y: y - size * 0.9 + outY * size * 0.4)
                let knee = CGPoint(x: start.x + outX * size * 1.3, y: start.y + outY * size * 1.3 - size * 0.75)
                let foot = CGPoint(x: knee.x + outX * size * 0.8, y: knee.y + size * 1.5 + outY * size * 0.5)
                legs.move(to: start)
                legs.addQuadCurve(to: knee, control: CGPoint(x: start.x + outX * size * 0.5, y: knee.y))
                legs.addLine(to: foot)
            }
        }
        let abdomen = Path(ellipseIn: CGRect(x: x - size * 0.75, y: y - size * 0.4, width: size * 1.5, height: size * 1.7))
        let head = circle(x, y - size * 0.9, size * 0.5)
        // A light rim first, so a black spider still shows on a dark background.
        let rim = GraphicsContext.Shading.color(Color(red: 235 / 255, green: 235 / 255, blue: 235 / 255, opacity: 110 / 255))
        let rimLine = StrokeStyle(lineWidth: 1.1, lineCap: .round, lineJoin: .round)
        context.stroke(legs, with: rim, style: rimLine)
        context.stroke(abdomen, with: rim, style: rimLine)
        context.stroke(head, with: rim, style: rimLine)
        let body = GraphicsContext.Shading.color(spiderBody.color())
        context.stroke(legs, with: body, style: StrokeStyle(lineWidth: 0.6, lineCap: .round, lineJoin: .round))
        context.fill(abdomen, with: body)
        context.fill(head, with: body)
        context.fill(circle(x - size * 0.25, y + size * 0.15, size * 0.18), with: .color(white.color(alpha: 140)))
    }

    // MARK: - Dust bunnies

    /// Fluff on the floor beside a pile that has volumes old enough for a spider, as many as fit
    /// beside the bottom of the pile.
    private static func drawDustBunnies(_ context: inout GraphicsContext, _ placed: [Placed]) {
        guard let lowest = placed.first else { return }
        // One per three such volumes, rounded up: a big pile gets a few, not a carpet of them.
        let ancient = placed.filter { $0.stage == .spider }
        let leaders = stride(from: 0, to: ancient.count, by: 3).map { ancient[$0] }
        var right = (placed.prefix(3).map(\.left).min() ?? lowest.left) - 3
        for book in leaders {
            let radius = 4.2 + (book.look.thickness - 0.8) * 4
            let middle = right - radius
            if middle - radius < 0 { return }
            drawDustBunny(&context, x: middle, y: lowest.bottom - radius * 0.75, radius: radius, seed: book.look.seed)
            right = middle - radius - (2 + CGFloat(book.look.seed & 7))
        }
    }

    private static func drawDustBunny(_ context: inout GraphicsContext, x: CGFloat, y: CGFloat, radius: CGFloat, seed: UInt64) {
        var random = SeededRandom(seed: seed ^ 0xB0B)
        var shadow = context
        shadow.addFilter(.blur(radius: 1))
        shadow.fill(
            Path(ellipseIn: CGRect(x: x - radius * 1.1, y: y + radius * 0.55, width: radius * 2.2, height: radius * 0.4)),
            with: .color(black.color(alpha: 70))
        )
        var fluff = context
        fluff.addFilter(.blur(radius: 0.6))
        for _ in 0..<14 {
            let angle = random.nextFloat() * 2 * .pi
            let distance = radius * 0.55 * random.nextFloat()
            let colour = random.nextBool() ? dustLight : dust
            let alpha = 150 + random.nextInt(90)
            let size = radius * (0.35 + random.nextFloat() * 0.3)
            fluff.fill(
                circle(x + cos(angle) * distance, y + sin(angle) * distance * 0.8, size),
                with: .color(colour.color(alpha: alpha))
            )
        }
        // Stray fibres sticking out.
        var fibres = Path()
        for _ in 0..<9 {
            let angle = random.nextFloat() * 2 * .pi
            let from = radius * 0.6
            let to = radius * (1.05 + random.nextFloat() * 0.45)
            fibres.move(to: CGPoint(x: x + cos(angle) * from, y: y + sin(angle) * from * 0.8))
            fibres.addLine(to: CGPoint(x: x + cos(angle + 0.2) * to, y: y + sin(angle + 0.2) * to * 0.8))
        }
        context.stroke(
            fibres,
            with: .color(dustLight.color(alpha: 0xB4)),
            style: StrokeStyle(lineWidth: 0.35, lineCap: .round)
        )
    }

    // MARK: - Colours

    /// `colour` bleached and greyed the longer the volume has waited.
    private static func faded(_ colour: Rgb, _ stage: DustStage) -> Rgb {
        let amount: CGFloat =
            switch stage {
            case .clean: 0
            case .light: 0.06
            case .thick: 0.2
            case .cobweb: 0.32
            case .spider: 0.45
            }
        return colour.mixed(with: dust, amount)
    }

    private static func circle(_ x: CGFloat, _ y: CGFloat, _ radius: CGFloat) -> Path {
        Path(ellipseIn: CGRect(x: x - radius, y: y - radius, width: radius * 2, height: radius * 2))
    }
}

/// An opaque colour that can be mixed with another, as the drawing needs.
private struct Rgb {
    let red: CGFloat
    let green: CGFloat
    let blue: CGFloat

    init(_ hex: UInt32) {
        red = CGFloat((hex >> 16) & 0xFF) / 255
        green = CGFloat((hex >> 8) & 0xFF) / 255
        blue = CGFloat(hex & 0xFF) / 255
    }

    private init(red: CGFloat, green: CGFloat, blue: CGFloat) {
        self.red = red
        self.green = green
        self.blue = blue
    }

    func mixed(with other: Rgb, _ amount: CGFloat) -> Rgb {
        Rgb(
            red: red + (other.red - red) * amount,
            green: green + (other.green - green) * amount,
            blue: blue + (other.blue - blue) * amount
        )
    }

    /// With `alpha` out of 255.
    func color(alpha: Int = 255) -> Color {
        Color(red: red, green: green, blue: blue, opacity: CGFloat(alpha) / 255)
    }
}

private extension Array {
    subscript(safe index: Int) -> Element? { indices.contains(index) ? self[index] : nil }
}
