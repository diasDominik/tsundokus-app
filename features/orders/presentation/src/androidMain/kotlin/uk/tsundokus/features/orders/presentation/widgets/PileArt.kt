package uk.tsundokus.features.orders.presentation.widgets

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * The pile widget's picture: every unread volume as a book lying in a stack, the longest-waiting at
 * the bottom, gathering dust, cobwebs and finally a spider the longer it waits.
 *
 * Glance has no canvas, so this is drawn into a bitmap at the widget's exact size. The words go
 * above the pile: nothing is drawn above [draw]'s `top`, so a pile taller than the room under them
 * runs out of the top of its space. Its colours don't
 * follow the theme — books have their own — and the cobwebs and the spider carry both a light and a
 * dark edge, so the one picture reads on a light card, a dark card and the bare wallpaper alike.
 */
internal object PileArt {
    /** Thickest a volume is drawn, when the pile is short. */
    private const val MAX_THICKNESS_DP = 13f

    /** Thinnest: a pile taller than the widget at this thickness runs out of the top. */
    private const val MIN_THICKNESS_DP = 3f

    /** The pile never gets wider than this, so a wide widget doesn't get absurdly long books. */
    private const val MAX_PILE_WIDTH_DP = 140f

    /** Of the widget's width the pile takes, at the right; dust bunnies gather on the left. */
    private const val PILE_WIDTH_FRACTION = 0.72f

    /** How far a volume being read is pulled out of the pile, of the pile's width. */
    private const val PULLED_OUT = 0.2f

    private val SPINES =
        intArrayOf(
            0xFF8E2C2C.toInt(), // oxblood
            0xFF1F3A5F.toInt(), // navy
            0xFF2E5E4E.toInt(), // forest
            0xFFC9A227.toInt(), // mustard
            0xFF2A7F86.toInt(), // teal
            0xFF5B3A6B.toInt(), // plum
            0xFFC4622D.toInt(), // burnt orange
            0xFF4A5560.toInt(), // slate
            0xFFE9E3D5.toInt(), // manga white
            0xFF7A2E4A.toInt(), // wine
            0xFF3F6E9E.toInt(), // blue
            0xFF6B7F3A.toInt(), // olive
        )

    private const val DUST = 0xFF857E70.toInt()
    private const val DUST_LIGHT = 0xFFA9A291.toInt()
    private const val PAGES = 0xFFEDE6D3.toInt()
    private const val RIBBON = 0xFFC0392B.toInt()
    private const val THREAD = 0xFFF4F4F4.toInt()
    private const val SPIDER_BODY = 0xFF151515.toInt()

    /** Where the last web on each side of the pile reaches up to, so the next doesn't overlap it. */
    private class Webs {
        var leftTop = Float.MAX_VALUE
        var rightTop = Float.MAX_VALUE
    }

    /** One volume where it ends up in the picture. */
    private class Placed(
        val book: WidgetPileBook,
        val look: PileBookLook,
        val stage: DustStage,
        val left: Float,
        val right: Float,
        val top: Float,
        val bottom: Float,
    ) {
        val thickness: Float get() = bottom - top
    }

    fun draw(
        widthPx: Int,
        heightPx: Int,
        density: Float,
        books: List<WidgetPileBook>,
        today: String,
        top: Float = 0f,
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(max(widthPx, 1), max(heightPx, 1), Bitmap.Config.ARGB_8888)
        if (books.isEmpty()) return bitmap
        val canvas = Canvas(bitmap)
        canvas.clipRect(0f, top, widthPx.toFloat(), heightPx.toFloat())
        val placed = place(widthPx.toFloat(), heightPx.toFloat(), top, density, books, today)
        drawDustBunnies(canvas, density, placed)
        // Bottom first: each volume lies on, and covers part of, the one below.
        val webs = Webs()
        placed.forEachIndexed { index, book ->
            drawBook(canvas, density, book)
            placed.getOrNull(index - 1)?.let { below -> drawWebBetween(canvas, density, below, book, webs) }
        }
        placed.lastOrNull { it.stage == DustStage.SPIDER }?.let { home ->
            drawSpider(
                canvas,
                density,
                home,
                placed.getOrNull(placed.indexOf(home) + 1),
                floor = heightPx.toFloat(),
            )
        }
        return bitmap
    }

    private fun place(
        width: Float,
        height: Float,
        top: Float,
        density: Float,
        books: List<WidgetPileBook>,
        today: String,
    ): List<Placed> {
        val pileWidth = min(width * PILE_WIDTH_FRACTION, MAX_PILE_WIDTH_DP * density)
        // Room on the right for the longest volume shifted furthest that way.
        val middle = width - pileWidth * 0.58f - density * 2f
        val looks = books.map { it.look(SPINES.size) }
        val fitting = (height - top) * 0.94f / looks.sumOf { it.thickness.toDouble() }.toFloat()
        val unit = fitting.coerceIn(MIN_THICKNESS_DP * density, MAX_THICKNESS_DP * density)
        var bottom = height - density
        return books.zip(looks).mapNotNull { (book, look) ->
            // Out of the top of its space: the rest of the pile is not drawn.
            if (bottom <= top) return@mapNotNull null
            val bookTop = bottom - unit * look.thickness
            val length = pileWidth * look.length
            val centre = middle + pileWidth * look.shift - if (book.reading) pileWidth * PULLED_OUT else 0f
            Placed(book, look, book.dustOn(today), centre - length / 2f, centre + length / 2f, bookTop, bottom)
                .also { bottom = bookTop }
        }
    }

    private fun drawBook(
        canvas: Canvas,
        density: Float,
        book: Placed,
    ) {
        val sag = if (book.stage == DustStage.SPIDER) min(book.thickness * 0.22f, density * 2f) else 0f
        val radius = min(book.thickness * 0.25f, density * 2f)
        val outline = bookOutline(book, radius, sag)

        // Shadow on the book underneath.
        canvas.drawPath(
            Path(outline).apply { offset(0f, density * 0.8f) },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(90, 0, 0, 0)
                maskFilter = BlurMaskFilter(density * 1.4f, BlurMaskFilter.Blur.NORMAL)
            },
        )

        val base = faded(SPINES[book.look.colour], book.stage)
        canvas.save()
        canvas.clipPath(outline)
        if (book.look.pagesOut) {
            drawPageEdge(
                canvas,
                density,
                book,
                base,
            )
        } else {
            drawSpine(canvas, density, book, base)
        }
        // Light catching the rounded top of the spine.
        canvas.drawRect(
            book.left,
            book.top + book.thickness * 0.12f,
            book.right,
            book.top + book.thickness * 0.24f,
            Paint().apply { color = Color.argb(45, 255, 255, 255) },
        )
        drawDust(canvas, density, book)
        canvas.restore()

        if (book.book.reading) drawRibbon(canvas, density, book)
    }

    /** A rounded rectangle, bowed down in the middle when [sag] says it has started to give way. */
    private fun bookOutline(
        book: Placed,
        radius: Float,
        sag: Float,
    ): Path {
        if (sag == 0f) {
            return Path().apply {
                addRoundRect(
                    RectF(book.left, book.top, book.right, book.bottom),
                    radius,
                    radius,
                    Path.Direction.CW,
                )
            }
        }
        val middle = (book.left + book.right) / 2f
        return Path().apply {
            moveTo(book.left + radius, book.top)
            quadTo(middle, book.top + sag * 2f, book.right - radius, book.top)
            quadTo(book.right, book.top, book.right, book.top + radius)
            lineTo(book.right, book.bottom - radius)
            quadTo(book.right, book.bottom, book.right - radius, book.bottom)
            quadTo(middle, book.bottom + sag * 2f, book.left + radius, book.bottom)
            quadTo(book.left, book.bottom, book.left, book.bottom - radius)
            lineTo(book.left, book.top + radius)
            quadTo(book.left, book.top, book.left + radius, book.top)
            close()
        }
    }

    private fun drawSpine(
        canvas: Canvas,
        density: Float,
        book: Placed,
        base: Int,
    ) {
        canvas.drawRect(
            book.left,
            book.top - density * 3f,
            book.right,
            book.bottom + density * 3f,
            Paint().apply { shader = roundness(book, base) },
        )
        // Bands near each end, and a label in the middle on a spine thick enough to show one.
        val band =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color =
                    mix(base, Color.WHITE, 0.45f) and 0x99FFFFFF.toInt()
            }
        val length = book.right - book.left
        listOf(0.09f, 0.91f).forEach { at ->
            val x = book.left + length * at
            canvas.drawRect(x - density * 0.6f, book.top, x + density * 0.6f, book.bottom, band)
        }
        if (book.thickness >= density * 6f) {
            val labelHeight = book.thickness * 0.42f
            val labelMiddle = (book.top + book.bottom) / 2f
            canvas.drawRoundRect(
                RectF(
                    book.left + length * 0.32f,
                    labelMiddle - labelHeight / 2f,
                    book.right - length * 0.32f,
                    labelMiddle + labelHeight / 2f,
                ),
                density,
                density,
                Paint(
                    Paint.ANTI_ALIAS_FLAG,
                ).apply { color = mix(base, Color.WHITE, 0.55f) and 0x88FFFFFF.toInt() },
            )
        }
    }

    /** A volume lying the other way: the cream block of its pages between the two covers. */
    private fun drawPageEdge(
        canvas: Canvas,
        density: Float,
        book: Placed,
        base: Int,
    ) {
        val pages = faded(PAGES, book.stage)
        canvas.drawRect(
            book.left,
            book.top - density * 3f,
            book.right,
            book.bottom + density * 3f,
            Paint().apply { shader = roundness(book, pages) },
        )
        val lines =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = mix(pages, Color.BLACK, 0.18f)
                strokeWidth = max(density * 0.3f, 1f)
            }
        var y = book.top + density * 1.2f
        while (y < book.bottom - density) {
            canvas.drawLine(book.left, y, book.right, y, lines)
            y += density * 1.1f
        }
        val cover = book.thickness * 0.13f
        val boards = Paint().apply { color = mix(base, Color.BLACK, 0.15f) }
        canvas.drawRect(book.left, book.top - density, book.right, book.top + cover, boards)
        canvas.drawRect(book.left, book.bottom - cover, book.right, book.bottom + density, boards)
    }

    /** Lighter at the top, darker underneath: a spine is rounded, not flat. */
    private fun roundness(
        book: Placed,
        colour: Int,
    ): Shader =
        LinearGradient(
            0f,
            book.top,
            0f,
            book.bottom,
            intArrayOf(mix(colour, Color.WHITE, 0.2f), colour, mix(colour, Color.BLACK, 0.32f)),
            floatArrayOf(0f, 0.45f, 1f),
            Shader.TileMode.CLAMP,
        )

    private fun drawDust(
        canvas: Canvas,
        density: Float,
        book: Placed,
    ) {
        val (veil, lip, grains) =
            when (book.stage) {
                DustStage.CLEAN -> return
                DustStage.LIGHT -> Triple(0, 0.7f, 0.6f)
                DustStage.THICK -> Triple(45, 1.2f, 1.6f)
                DustStage.COBWEB -> Triple(75, 1.6f, 2.4f)
                DustStage.SPIDER -> Triple(105, 2f, 3.2f)
            }
        if (veil > 0) {
            canvas.drawRect(
                book.left,
                book.top,
                book.right,
                book.bottom,
                Paint().apply { color = DUST and 0x00FFFFFF or (veil shl 24) },
            )
        }
        val random = Random(book.look.seed)
        val length = book.right - book.left
        // A soft, uneven layer settled along the top edge; never more than a slice of a thin volume.
        val depth = min(density * lip, book.thickness * 0.3f)
        val lipPath =
            Path().apply {
                moveTo(book.left, book.top)
                var x = book.left
                var y = book.top + depth * 0.6f
                lineTo(x, y)
                while (x < book.right) {
                    val nextX = min(x + density * (2f + random.nextFloat() * 3f), book.right)
                    val nextY = book.top + depth * (0.45f + random.nextFloat() * 0.55f)
                    quadTo((x + nextX) / 2f, book.top + depth * (0.7f + random.nextFloat() * 0.6f), nextX, nextY)
                    x = nextX
                    y = nextY
                }
                lineTo(book.right, book.top)
                close()
            }
        canvas.drawPath(
            lipPath,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = DUST_LIGHT and 0xC8FFFFFF.toInt()
                maskFilter = BlurMaskFilter(density * 0.35f, BlurMaskFilter.Blur.NORMAL)
            },
        )
        // Grime: specks on the top edge at first, all over once it has been there a while.
        val grain = Paint(Paint.ANTI_ALIAS_FLAG)
        val count = (length / density * grains).toInt()
        val reach = if (book.stage == DustStage.LIGHT) 0.35f else 1f
        repeat(count) {
            val x = book.left + random.nextFloat() * length
            val y = book.top + book.thickness * reach * random.nextFloat() * random.nextFloat()
            grain.color = (if (random.nextInt(4) == 0) DUST_LIGHT else DUST) and 0x00FFFFFF or
                ((70 + random.nextInt(120)) shl 24)
            canvas.drawCircle(x, y, density * (0.18f + random.nextFloat() * 0.32f), grain)
        }
    }

    /** The bookmark hanging out of a volume being read, notched at the end. */
    private fun drawRibbon(
        canvas: Canvas,
        density: Float,
        book: Placed,
    ) {
        val x = book.left + (book.right - book.left) * 0.16f
        val width = density * 2.4f
        val end = book.bottom + book.thickness * 0.9f + density * 4f
        val ribbon =
            Path().apply {
                moveTo(x, book.bottom - density)
                lineTo(x + width, book.bottom - density)
                lineTo(x + width, end)
                lineTo(x + width / 2f, end - width * 0.8f)
                lineTo(x, end)
                close()
            }
        canvas.drawPath(
            Path(ribbon).apply { offset(density * 0.5f, density * 0.6f) },
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(70, 0, 0, 0) },
        )
        canvas.drawPath(ribbon, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = RIBBON })
    }

    /**
     * A cobweb in the corner where [upper] lies on [lower], on the side where [lower] sticks out more.
     * Only once [lower] has waited long enough for spiders to bother.
     */
    private fun drawWebBetween(
        canvas: Canvas,
        density: Float,
        lower: Placed,
        upper: Placed,
        webs: Webs,
    ) {
        if (lower.stage < DustStage.COBWEB) return
        val leftOverhang = upper.left - lower.left
        val rightOverhang = lower.right - upper.right
        val onLeft = leftOverhang >= rightOverhang
        val overhang = max(leftOverhang, rightOverhang)
        val size =
            (overhang * 1.5f).coerceIn(
                density * 8f,
                max(min(upper.thickness * 2.6f, density * 18f), density * 8f),
            )
        // A thin pile has a corner every few pixels; one web per stretch of it is plenty.
        val lastTop = if (onLeft) webs.leftTop else webs.rightTop
        if (lower.top > lastTop - density * 2f) return
        if (onLeft) webs.leftTop = lower.top - size else webs.rightTop = lower.top - size
        val cornerX = if (onLeft) upper.left else upper.right
        drawCornerWeb(canvas, density, cornerX, lower.top, size, towardsLeft = onLeft, seed = lower.look.seed)
    }

    /** Threads fanned out from a corner, with sagging rings between them. */
    private fun drawCornerWeb(
        canvas: Canvas,
        density: Float,
        cornerX: Float,
        cornerY: Float,
        size: Float,
        towardsLeft: Boolean,
        seed: Long,
    ) {
        val random = Random(seed xor 0x5EB)
        val spokes = 5
        val direction = if (towardsLeft) -1f else 1f
        // From along the lower book's top (0°) up to along the upper book's side (90°).
        val ends =
            (0 until spokes).map { spoke ->
                val angle = (spoke / (spokes - 1f)) * (PI / 2).toFloat()
                val reach = size * (0.85f + random.nextFloat() * 0.25f)
                Pair(cornerX + direction * cos(angle) * reach, cornerY - sin(angle) * reach)
            }
        val web = Path()
        ends.forEach { (x, y) ->
            web.moveTo(cornerX, cornerY)
            web.lineTo(x, y)
        }
        listOf(0.35f, 0.62f, 0.9f).forEach { ring ->
            ends.zipWithNext().forEachIndexed { index, (from, to) ->
                val fromX = cornerX + (from.first - cornerX) * ring
                val fromY = cornerY + (from.second - cornerY) * ring
                val toX = cornerX + (to.first - cornerX) * ring
                val toY = cornerY + (to.second - cornerY) * ring
                // Sag towards the corner, a little different on every ring.
                val pull = 0.22f + ((index + ring * 10).toInt() % 3) * 0.05f
                val controlX = (fromX + toX) / 2f + (cornerX - (fromX + toX) / 2f) * pull
                val controlY = (fromY + toY) / 2f + (cornerY - (fromY + toY) / 2f) * pull
                if (index == 0) web.moveTo(fromX, fromY)
                web.quadTo(controlX, controlY, toX, toY)
            }
        }
        // A dark hairline under the light thread, so the web shows on any background.
        canvas.drawPath(
            Path(web).apply { offset(density * 0.35f, density * 0.35f) },
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = density * 0.55f
                color = Color.argb(90, 0, 0, 0)
            },
        )
        canvas.drawPath(
            web,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = density * 0.55f
                color = THREAD and 0xEEFFFFFF.toInt()
            },
        )
    }

    /**
     * The spider: on its web on the highest volume that has waited long enough for one, or, with no
     * web to sit in, dangling on a thread off that volume's end.
     */
    private fun drawSpider(
        canvas: Canvas,
        density: Float,
        home: Placed,
        above: Placed?,
        floor: Float,
    ) {
        val size = density * 3f
        val (x, y) =
            if (above == null) {
                // Top of the pile: walking across the top of it.
                Pair(home.left + (home.right - home.left) * 0.7f, home.top - size * 1.1f)
            } else {
                // Hanging off the end that sticks out more, on a thread from under the volume above.
                val onLeft = above.left - home.left >= home.right - above.right
                val threadX = if (onLeft) home.left - size * 1.5f else home.right + size * 1.5f
                // Down past the volume, but not through the floor.
                val bottom = min(home.bottom + size * 3.5f, floor - size * 1.5f)
                canvas.drawLine(
                    threadX,
                    home.top - size,
                    threadX,
                    bottom - size * 1.2f,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = THREAD and 0xCCFFFFFF.toInt()
                        strokeWidth = density * 0.4f
                    },
                )
                Pair(threadX, bottom)
            }
        // Four legs a side from the front body: the front pair reaching forward, the back pair back,
        // each bent up at the knee and down to the foot.
        val legs =
            Path().apply {
                listOf(-1f, 1f).forEach { side ->
                    listOf(-62f, -22f, 18f, 52f).forEach { degrees ->
                        val angle = Math.toRadians(degrees.toDouble())
                        val outX = cos(angle).toFloat() * side
                        val outY = sin(angle).toFloat()
                        val startX = x + outX * size * 0.4f
                        val startY = y - size * 0.9f + outY * size * 0.4f
                        val kneeX = startX + outX * size * 1.3f
                        val kneeY = startY + outY * size * 1.3f - size * 0.75f
                        val footX = kneeX + outX * size * 0.8f
                        val footY = kneeY + size * 1.5f + outY * size * 0.5f
                        moveTo(startX, startY)
                        quadTo(startX + outX * size * 0.5f, kneeY, kneeX, kneeY)
                        lineTo(footX, footY)
                    }
                }
            }
        // A light rim first, so a black spider still shows on a dark background.
        val rim =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(110, 235, 235, 235)
                style = Paint.Style.STROKE
                strokeWidth = density * 1.1f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            }
        canvas.drawPath(legs, rim)
        canvas.drawOval(x - size * 0.75f, y - size * 0.4f, x + size * 0.75f, y + size * 1.3f, rim)
        canvas.drawCircle(x, y - size * 0.9f, size * 0.5f, rim)
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = SPIDER_BODY }
        canvas.drawPath(
            legs,
            Paint(body).apply {
                style = Paint.Style.STROKE
                strokeWidth = density * 0.6f
                strokeCap = Paint.Cap.ROUND
                strokeJoin = Paint.Join.ROUND
            },
        )
        canvas.drawOval(x - size * 0.75f, y - size * 0.4f, x + size * 0.75f, y + size * 1.3f, body)
        canvas.drawCircle(x, y - size * 0.9f, size * 0.5f, body)
        canvas.drawCircle(
            x - size * 0.25f,
            y + size * 0.15f,
            size * 0.18f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(140, 255, 255, 255) },
        )
    }

    /**
     * Fluff on the floor beside a pile that has volumes old enough for a spider, as many as fit
     * beside the bottom of the pile.
     */
    private fun drawDustBunnies(
        canvas: Canvas,
        density: Float,
        placed: List<Placed>,
    ) {
        // One per three such volumes, rounded up: a big pile gets a few, not a carpet of them.
        val ancient = placed.filter { it.stage == DustStage.SPIDER }.chunked(3).map { it.first() }
        val bottom = placed.firstOrNull() ?: return
        var right = placed.take(3).minOf { it.left } - density * 3f
        ancient.forEach { book ->
            val radius = density * (4.2f + (book.look.thickness - 0.8f) * 4f)
            val middle = right - radius
            if (middle - radius < 0f) return
            drawDustBunny(canvas, density, middle, bottom.bottom - radius * 0.75f, radius, book.look.seed)
            right = middle - radius - density * (2f + (book.look.seed and 7L).toFloat())
        }
    }

    private fun drawDustBunny(
        canvas: Canvas,
        density: Float,
        x: Float,
        y: Float,
        radius: Float,
        seed: Long,
    ) {
        val random = Random(seed xor 0xB0B)
        canvas.drawOval(
            x - radius * 1.1f,
            y + radius * 0.55f,
            x + radius * 1.1f,
            y + radius * 0.95f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.argb(70, 0, 0, 0)
                maskFilter = BlurMaskFilter(density, BlurMaskFilter.Blur.NORMAL)
            },
        )
        val fluff =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(density * 0.6f, BlurMaskFilter.Blur.NORMAL)
            }
        repeat(14) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val distance = radius * 0.55f * random.nextFloat()
            fluff.color = (if (random.nextBoolean()) DUST_LIGHT else DUST) and 0x00FFFFFF or
                ((150 + random.nextInt(90)) shl 24)
            canvas.drawCircle(
                x + cos(angle) * distance,
                y + sin(angle) * distance * 0.8f,
                radius * (0.35f + random.nextFloat() * 0.3f),
                fluff,
            )
        }
        // Stray fibres sticking out.
        val fibre =
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = density * 0.35f
                strokeCap = Paint.Cap.ROUND
                color = DUST_LIGHT and 0xB4FFFFFF.toInt()
            }
        repeat(9) {
            val angle = random.nextFloat() * 2f * PI.toFloat()
            val from = radius * 0.6f
            val to = radius * (1.05f + random.nextFloat() * 0.45f)
            canvas.drawLine(
                x + cos(angle) * from,
                y + sin(angle) * from * 0.8f,
                x + cos(angle + 0.2f) * to,
                y + sin(angle + 0.2f) * to * 0.8f,
                fibre,
            )
        }
    }

    /** [colour] bleached and greyed the longer the volume has waited. */
    private fun faded(
        colour: Int,
        stage: DustStage,
    ): Int =
        mix(
            colour,
            DUST,
            when (stage) {
                DustStage.CLEAN -> 0f
                DustStage.LIGHT -> 0.06f
                DustStage.THICK -> 0.2f
                DustStage.COBWEB -> 0.32f
                DustStage.SPIDER -> 0.45f
            },
        )

    private fun mix(
        from: Int,
        to: Int,
        amount: Float,
    ): Int {
        fun channel(shift: Int): Int {
            val a = (from shr shift) and 0xFF
            val b = (to shr shift) and 0xFF
            return (a + (b - a) * amount).toInt().coerceIn(0, 255)
        }
        return (0xFF shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }
}
