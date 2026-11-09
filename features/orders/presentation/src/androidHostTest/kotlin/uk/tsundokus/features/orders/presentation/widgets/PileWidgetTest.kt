package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

// 2x1 on a small launcher grid, 2x1 on a Pixel, and 2x2 on a Pixel.
private val SHORT = DpSize(110.dp, 60.dp)
private val PIXEL_2X1 = DpSize(150.dp, 108.dp)
private val PIXEL_2X2 = DpSize(150.dp, 200.dp)

private val pile =
    PileUi(
        heading = "Your pile",
        count = 12,
        countLabel = "volumes to read",
        detail = "2 being read · oldest 30 days",
        message = null,
    )

@RunWith(AndroidJUnit4::class)
class PileWidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `a short widget shows the count only`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(SHORT)
            provideComposable { PileContent(pile) }

            onNode(hasTestTag("count")).assert(hasText("12"))
            onNode(hasText("volumes to read")).assertExists()
            onNode(hasText("Your pile")).assertDoesNotExist()
            onNode(hasTestTag("detail")).assertDoesNotExist()
        }

    @Test
    fun `a taller widget adds the heading and the detail line`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X1)
            provideComposable { PileContent(pile) }

            onNode(hasText("Your pile")).assertExists()
            onNode(hasTestTag("detail")).assert(hasText("2 being read · oldest 30 days"))
        }

    @Test
    fun `a 2x2 draws the pile next to the count`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable {
                PileContent(pile.copy(books = listOf(WidgetPileBook("1", "2026-01-01")), today = "2026-11-08"))
            }

            onNode(hasTestTag("count")).assert(hasText("12"))
            onNode(hasTestTag("detail")).assert(hasText("2 being read · oldest 30 days"))
        }

    @Test
    fun `a picture is drawn at the screen's density while it fits the budget`() {
        // A 2x2 at 2.625 (Pixel 8) fits as it is.
        assertEquals(2.625f, artScale(126f, 176f, density = 2.625f))
        // At 3.0 it would not quite: a little coarser. A 4x4 at 3.0 a lot coarser.
        listOf(126f to 176f, 330f to 420f).forEach { (width, height) ->
            val scale = artScale(width, height, density = 3f)
            assertTrue(scale < 3f)
            assertTrue(width * scale * height * scale <= 160_001f)
        }
    }

    @Test
    fun `with nothing in the pile the message stands in`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(SHORT)
            provideComposable { PileContent(PileUi("Your pile", null, "", null, "All caught up")) }

            onNode(hasTestTag("message")).assert(hasText("All caught up"))
            onNode(hasTestTag("count")).assertDoesNotExist()
        }

    @Test
    fun `the heading and the details line can each be turned off`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X1)
            provideComposable { PileContent(pile, WidgetStyle(showHeading = false, showDetail = false)) }

            onNode(hasText("Your pile")).assertDoesNotExist()
            onNode(hasTestTag("detail")).assertDoesNotExist()
            onNode(hasTestTag("count")).assert(hasText("12"))
        }
}
