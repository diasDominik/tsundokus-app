package uk.tsundokus.features.orders.presentation.widgets

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.hasStartActivityClickAction
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import uk.tsundokus.features.orders.presentation.navigation.orderDeepLink
import kotlin.test.Test

private fun line(
    id: String,
    phrase: ArrivalPhrase = ArrivalPhrase.ARRIVES,
) = ArrivalLine(
    id = id,
    title = "Berserk",
    volume = id,
    detail = "Arrives Thu 9 Oct",
    phrase = phrase,
)

// A 2x2 on a Pixel.
private val PIXEL_2X2 = DpSize(150.dp, 200.dp)

private val fiveArrivals =
    NextArrivalsUi(
        "Next arrivals",
        (1..5).map {
            line("$it")
        },
        signedOutMessage = null,
        nothingMessage = "Nothing on the way",
    )

private operator fun NextArrivalsUi.plus(extra: ArrivalLine) = copy(lines = lines + extra)

@RunWith(AndroidJUnit4::class)
class NextArrivalsWidgetTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `every arrival is listed for the list to scroll through`() =
        runGlanceAppWidgetUnitTest {
            val twelve = fiveArrivals.copy(lines = (1..12).map { line("$it") })
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable { NextArrivalsContent(twelve) }

            // More than the ten child slots of a plain Column: only a lazy list keeps the twelfth.
            onNode(hasTestTag("arrival-1")).assertExists()
            onNode(hasTestTag("arrival-12")).assertExists()
        }

    @Test
    fun `a tap on an arrival opens that order`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable { NextArrivalsContent(fiveArrivals) }

            onNode(
                hasTestTag("arrival-1"),
            ).assert(hasStartActivityClickAction(openAppIntent(context, orderDeepLink("1"))))
        }

    @Test
    fun `with nothing to list the message stands in`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable {
                NextArrivalsContent(
                    NextArrivalsUi(
                        "Next arrivals",
                        emptyList(),
                        signedOutMessage = null,
                        nothingMessage = "Nothing on the way",
                    ),
                )
            }

            onNode(hasTestTag("message")).assert(hasText("Nothing on the way"))
        }

    @Test
    fun `without its heading the widget starts with the first arrival`() =
        runGlanceAppWidgetUnitTest {
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable { NextArrivalsContent(fiveArrivals, WidgetStyle(showHeading = false)) }

            onNode(hasText("Next arrivals")).assertDoesNotExist()
            onNode(hasTestTag("arrival-1")).assertExists()
        }

    @Test
    fun `releases can be left out to show only parcels on the way`() =
        runGlanceAppWidgetUnitTest {
            val ui = fiveArrivals.copy(lines = listOf(line("1", ArrivalPhrase.RELEASES), line("2")))
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable { NextArrivalsContent(ui, WidgetStyle(showReleases = false)) }

            onNode(hasTestTag("arrival-1")).assertDoesNotExist()
            onNode(hasTestTag("arrival-2")).assertExists()
        }

    @Test
    fun `with only releases left out the widget says nothing is on the way`() =
        runGlanceAppWidgetUnitTest {
            val ui = fiveArrivals.copy(lines = listOf(line("1", ArrivalPhrase.RELEASES)))
            setContext(context)
            setAppWidgetSize(PIXEL_2X2)
            provideComposable { NextArrivalsContent(ui, WidgetStyle(showReleases = false)) }

            onNode(hasTestTag("message")).assert(hasText("Nothing on the way"))
        }
}
