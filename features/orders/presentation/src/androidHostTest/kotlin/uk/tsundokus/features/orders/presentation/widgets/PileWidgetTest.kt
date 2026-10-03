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

// 2x1 on a small launcher grid, and 2x1 on a Pixel.
private val SHORT = DpSize(110.dp, 60.dp)
private val PIXEL_2X1 = DpSize(150.dp, 108.dp)

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
