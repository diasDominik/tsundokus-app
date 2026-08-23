package uk.tsundokus.features.orders.presentation.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class OrderFormattersDateTest {
    @Test
    fun `epoch millis round trips through iso`() {
        val dates = listOf("1970-01-01", "2000-02-29", "2026-08-23", "2026-12-31", "2100-03-01")
        dates.forEach { iso ->
            val millis = epochMillisFromIso(iso)
            assertEquals(iso, millis?.let(::isoFromEpochMillis), "round trip failed for $iso")
        }
    }

    @Test
    fun `epoch day zero is the unix epoch`() {
        assertEquals(0L, epochMillisFromIso("1970-01-01"))
        assertEquals("1970-01-01", isoFromEpochMillis(0L))
    }

    @Test
    fun `mid day millis floor to their date`() {
        val noon = epochMillisFromIso("2026-08-23")!! + 43_200_000L
        assertEquals("2026-08-23", isoFromEpochMillis(noon))
    }

    @Test
    fun `malformed dates yield null`() {
        listOf("", "2026-08", "2026-13-01", "not-a-date", "2026-00-10").forEach { iso ->
            assertNull(epochMillisFromIso(iso), "expected null for '$iso'")
        }
    }
}
