package uk.tsundokus.features.orders.presentation.widgets

import kotlin.test.Test
import kotlin.test.assertEquals

private const val TODAY = "2026-10-03"

private fun arrival(
    status: String,
    date: String,
) = WidgetArrival(id = "a", title = "Berserk", volume = "42", date = date, status = status)

class ArrivalWordingTest {
    @Test
    fun `each state of an arrival gets its own sentence`() {
        assertEquals(ArrivalPhrase.RELEASES, arrival("ORDERED", "2026-10-10").phraseOn(TODAY))
        assertEquals(ArrivalPhrase.ARRIVES, arrival("SHIPPED", "2026-10-03").phraseOn(TODAY))
        assertEquals(ArrivalPhrase.DELAYED_TO, arrival("DELAYED", "2026-10-20").phraseOn(TODAY))
    }

    @Test
    fun `a parcel past its date is overdue whether shipped or delayed`() {
        assertEquals(ArrivalPhrase.OVERDUE, arrival("SHIPPED", "2026-10-01").phraseOn(TODAY))
        assertEquals(ArrivalPhrase.OVERDUE, arrival("DELAYED", "2026-10-02").phraseOn(TODAY))
    }

    @Test
    fun `today and tomorrow are named and other days are not`() {
        assertEquals(RelativeDay.TODAY, relativeDay("2026-10-03", TODAY))
        assertEquals(RelativeDay.TOMORROW, relativeDay("2026-10-04", TODAY))
        assertEquals(RelativeDay.OTHER, relativeDay("2026-10-05", TODAY))
        assertEquals(RelativeDay.OTHER, relativeDay("2026-10-02", TODAY))
    }

    @Test
    fun `tomorrow crosses the end of the month and the year`() {
        assertEquals(RelativeDay.TOMORROW, relativeDay("2026-11-01", "2026-10-31"))
        assertEquals(RelativeDay.TOMORROW, relativeDay("2027-01-01", "2026-12-31"))
    }
}
