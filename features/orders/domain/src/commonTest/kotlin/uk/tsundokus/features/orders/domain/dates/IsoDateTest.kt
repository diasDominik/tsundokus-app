package uk.tsundokus.features.orders.domain.dates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsoDateTest {
    @Test
    fun `a real date is split into its parts`() {
        assertEquals(IsoDate(2026, 10, 5), parseIsoDate("2026-10-05"))
    }

    @Test
    fun `leap days exist only in leap years`() {
        assertEquals(IsoDate(2028, 2, 29), parseIsoDate("2028-02-29"))
        assertEquals(IsoDate(2000, 2, 29), parseIsoDate("2000-02-29"))
        assertNull(parseIsoDate("2026-02-29"))
        // Divisible by 100 but not by 400: no leap day.
        assertNull(parseIsoDate("2100-02-29"))
    }

    @Test
    fun `dates that don't exist are refused`() {
        assertNull(parseIsoDate("2026-02-30"))
        assertNull(parseIsoDate("2026-04-31"))
        assertNull(parseIsoDate("2026-13-01"))
        assertNull(parseIsoDate("2026-00-10"))
    }

    @Test
    fun `anything else is refused`() {
        assertNull(parseIsoDate(""))
        assertNull(parseIsoDate("26-10-05"))
        assertNull(parseIsoDate("2026/10/05"))
        assertNull(parseIsoDate("2026-10-xx"))
    }

    @Test
    fun `adding days crosses months and years`() {
        assertEquals("2026-02-02", isoPlusDays("2026-01-30", 3))
        assertEquals("2027-01-02", isoPlusDays("2026-12-30", 3))
        assertEquals("2026-09-30", isoPlusDays("2026-10-01", -1))
    }

    @Test
    fun `adding days knows the leap years`() {
        assertEquals("2028-03-01", isoPlusDays("2028-02-27", 3))
        assertEquals("2026-03-02", isoPlusDays("2026-02-27", 3))
    }

    @Test
    fun `adding days lands on and leaves the 29th of February`() {
        assertEquals("2028-02-29", isoPlusDays("2028-02-26", 3))
        assertEquals("2028-03-03", isoPlusDays("2028-02-29", 3))
        assertEquals("2028-02-29", isoPlusDays("2028-02-28", 1))
        assertEquals("2026-03-01", isoPlusDays("2026-02-28", 1))
        assertEquals("2028-02-28", isoPlusDays("2028-02-29", -1))
    }

    @Test
    fun `the 29th of February in a year without one cannot be moved`() {
        // Would otherwise be read as 1 March and give 4 March.
        assertNull(isoPlusDays("2026-02-29", 3))
        assertNull(isoPlusDays("2100-02-29", 3))
    }

    @Test
    fun `an invalid date cannot be moved`() {
        assertNull(isoPlusDays("", 3))
        assertNull(isoPlusDays("soon", 3))
    }

    @Test
    fun `days between counts forwards and backwards`() {
        assertEquals(30, daysBetween("2026-09-02", "2026-10-02"))
        assertEquals(-1, daysBetween("2026-01-01", "2025-12-31"))
        assertEquals(366, daysBetween("2028-01-01", "2029-01-01"))
    }

    @Test
    fun `days between needs two real dates`() {
        assertNull(daysBetween("", "2026-10-02"))
        assertNull(daysBetween("2026-02-29", "2026-03-01"))
    }
}
