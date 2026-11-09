package uk.tsundokus.features.orders.presentation.widgets

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TODAY = "2026-11-08"

// A 2x2 on a Pixel 9 Pro, inside the card's padding.
private const val DENSITY = 3f
private const val WIDTH_DP = 126
private const val HEIGHT_DP = 176

private fun book(
    id: String,
    waitedDays: Int,
    reading: Boolean = false,
) = WidgetPileBook(id, isoPlusDays(TODAY, -waitedDays)!!, reading)

private val scenes =
    listOf(
        "fresh" to listOf(book("a", 10), book("b", 6), book("c", 3, reading = true), book("d", 1)),
        "every stage" to listOf(150, 100, 70, 45, 35, 20, 16, 9, 4, 1).mapIndexed { i, days -> book("s$i", days) },
        "neglected" to (0 until 14).map { book("n$it", 300 - it * 15) } + book("r", 20, reading = true),
        "huge" to (0 until 90).map { book("h$it", 365 - it * 4) },
    )

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PileArtTest {
    @Test
    fun `the picture fills the size it is drawn for`() {
        val bitmap = draw(scenes[1].second)

        assertEquals(WIDTH_DP * DENSITY.toInt(), bitmap.width)
        assertEquals(HEIGHT_DP * DENSITY.toInt(), bitmap.height)
        assertTrue(bitmap.hasSomethingDrawn())
    }

    @Test
    fun `an empty pile draws nothing`() {
        assertTrue(!draw(emptyList()).hasSomethingDrawn())
    }

    @Test
    fun `a pile too tall for the widget runs out of the top`() {
        val bitmap = draw(scenes.last().second)

        assertTrue((0 until bitmap.width).any { Color.alpha(bitmap.getPixel(it, 0)) > 0 })
    }

    /**
     * Not a check: writes every scene on a light and a dark card to build/pile-art.png, to look at
     * the drawing while working on it.
     */
    @Test
    fun `render the previews`() {
        val cell = (WIDTH_DP + 24) * DENSITY
        val row = (HEIGHT_DP + 24) * DENSITY
        val sheet = Bitmap.createBitmap((cell * scenes.size).toInt(), (row * 2).toInt(), Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet)
        canvas.drawColor(Color.rgb(120, 130, 140))
        listOf(Color.rgb(243, 237, 247), Color.rgb(29, 27, 32)).forEachIndexed { line, card ->
            scenes.forEachIndexed { column, (_, books) ->
                val left = column * cell + 6 * DENSITY
                val top = line * row + 6 * DENSITY
                canvas.drawRoundRect(
                    RectF(left, top, left + cell - 12 * DENSITY, top + row - 12 * DENSITY),
                    20 * DENSITY,
                    20 * DENSITY,
                    Paint(Paint.ANTI_ALIAS_FLAG).apply { color = card },
                )
                canvas.drawBitmap(draw(books), left + 6 * DENSITY, top + 6 * DENSITY, null)
            }
        }
        val file = File("build/pile-art.png")
        file.outputStream().use { sheet.compress(Bitmap.CompressFormat.PNG, 100, it) }
        println("Pile previews: ${file.absolutePath}")
    }
}

private fun draw(books: List<WidgetPileBook>): Bitmap =
    PileArt.draw((WIDTH_DP * DENSITY).toInt(), (HEIGHT_DP * DENSITY).toInt(), DENSITY, books, TODAY)

private fun Bitmap.hasSomethingDrawn(): Boolean =
    (0 until width step 4).any { x -> (0 until height step 4).any { y -> Color.alpha(getPixel(x, y)) > 0 } }
