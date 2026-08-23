package uk.tsundokus.features.orders.presentation.components

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the JVM format locale to prove [fmtDate] follows it. This lives in desktopTest because
 * only the JVM lets a test choose the locale; the iOS and wasm actuals delegate to the platform's
 * own medium-style formatter.
 */
class DateFormattingTest {
    private fun withLocale(
        locale: Locale,
        block: () -> Unit,
    ) {
        val original = Locale.getDefault(Locale.Category.FORMAT)
        try {
            Locale.setDefault(Locale.Category.FORMAT, locale)
            block()
        } finally {
            Locale.setDefault(Locale.Category.FORMAT, original)
        }
    }

    @Test
    fun `field order follows the locale, not a hardcoded pattern`() {
        withLocale(Locale.US) { assertEquals("Mar 7, 2026", fmtDate("2026-03-07")) }
        withLocale(Locale.UK) { assertEquals("7 Mar 2026", fmtDate("2026-03-07")) }
    }

    @Test
    fun `month names are localised`() {
        withLocale(Locale.GERMANY) { assertEquals("07.03.2026", fmtDate("2026-03-07")) }
    }

    @Test
    fun `blank and malformed input are passed through`() {
        withLocale(Locale.UK) {
            assertEquals("", fmtDate(""))
            assertEquals("not-a-date", fmtDate("not-a-date"))
            assertEquals("2026-13-01", fmtDate("2026-13-01"))
        }
    }
}
