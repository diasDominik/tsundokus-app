package uk.tsundokus.core.presentation.date

import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MonthFormatterTest {
    private lateinit var original: Locale

    @BeforeTest
    fun rememberLocale() {
        original = Locale.getDefault()
    }

    @AfterTest
    fun restoreLocale() {
        Locale.setDefault(original)
    }

    @Test
    fun `month and year follow the locale`() {
        Locale.setDefault(Locale.US)
        assertEquals("Mar 2026", formatShortMonthYear(2026, 3))

        Locale.setDefault(Locale.GERMANY)
        assertEquals("März 2026", formatShortMonthYear(2026, 3))

        Locale.setDefault(Locale.JAPAN)
        assertEquals("2026年3月", formatShortMonthYear(2026, 3))
    }

    @Test
    fun `narrow months follow the locale`() {
        Locale.setDefault(Locale.US)
        assertEquals(
            listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D"),
            (1..12).map(::formatNarrowMonth),
        )

        Locale.setDefault(Locale.JAPAN)
        assertEquals("3", formatNarrowMonth(3))
    }
}
