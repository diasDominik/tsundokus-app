package uk.tsundokus.features.orders.presentation.widgets

import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TODAY = "2026-11-08"

private fun waited(
    days: Int,
    reading: Boolean = false,
) = WidgetPileBook("b", isoPlusDays(TODAY, -days)!!, reading)

class PileBooksTest {
    @Test
    fun `dust settles in stages the longer a volume waits`() {
        val stages = listOf(0, 13, 14, 29, 30, 59, 60, 119, 120, 400).map { waited(it).dustOn(TODAY) }

        assertEquals(
            listOf(
                DustStage.CLEAN,
                DustStage.CLEAN,
                DustStage.LIGHT,
                DustStage.LIGHT,
                DustStage.THICK,
                DustStage.THICK,
                DustStage.COBWEB,
                DustStage.COBWEB,
                DustStage.SPIDER,
                DustStage.SPIDER,
            ),
            stages,
        )
    }

    @Test
    fun `a volume being read was just picked up so has no dust`() {
        assertEquals(DustStage.CLEAN, waited(300, reading = true).dustOn(TODAY))
    }

    @Test
    fun `a volume with no arrival date is not guessed at`() {
        assertEquals(DustStage.CLEAN, WidgetPileBook("b", "").dustOn(TODAY))
    }

    @Test
    fun `a volume looks the same every time and stays in bounds`() {
        val looks = (0 until 200).map { WidgetPileBook("order-$it", TODAY).look(colours = 12) }

        assertEquals(looks, (0 until 200).map { WidgetPileBook("order-$it", TODAY).look(colours = 12) })
        looks.forEach { look ->
            assertTrue(look.colour in 0 until 12)
            assertTrue(look.length in 0.72f..0.97f)
            assertTrue(look.shift in -0.08f..0.08f)
            assertTrue(look.thickness in 0.8f..1.2f)
        }
        assertTrue(looks.map { it.colour }.toSet().size > 6, "colours should vary between volumes")
    }
}
