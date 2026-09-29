package uk.tsundokus.core.domain.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppCurrencyTest {
    @Test
    fun `every currency in use is known`() {
        // At least: the server's list may add to the built-in one (see CurrencyCatalogUpdateTest).
        assertTrue(AppCurrency.all.size >= 155)
        assertEquals(
            AppCurrency.all.size,
            AppCurrency.all
                .map { it.code }
                .toSet()
                .size,
        )
    }

    @Test
    fun `no two currencies share a symbol`() {
        // Orders in several currencies sit side by side, so "$ 12" must never be ambiguous.
        val shared =
            AppCurrency.all
                .groupBy { it.symbol }
                .filterValues { it.size > 1 }
                .keys
        assertTrue(shared.isEmpty(), "Shared symbols: $shared")
    }

    @Test
    fun `the familiar ones keep their symbols`() {
        assertEquals("$", AppCurrency.USD.symbol)
        assertEquals("£", AppCurrency.GBP.symbol)
        assertEquals("€", AppCurrency.EUR.symbol)
        assertEquals("¥", AppCurrency.fromCode("JPY").symbol)
        assertEquals("CA$", AppCurrency.fromCode("CAD").symbol)
    }

    @Test
    fun `decimals follow the currency`() {
        assertEquals(2, AppCurrency.EUR.decimals)
        assertEquals(0, AppCurrency.fromCode("JPY").decimals)
        assertEquals(0, AppCurrency.fromCode("KRW").decimals)
        assertEquals(3, AppCurrency.fromCode("KWD").decimals)
    }

    @Test
    fun `each currency is written where its countries write it`() {
        assertEquals("12.99 €", AppCurrency.EUR.format("12.99"))
        assertEquals("$12.99", AppCurrency.USD.format("12.99"))
        assertEquals("£12.99", AppCurrency.GBP.format("12.99"))
        assertEquals("¥1200", AppCurrency.fromCode("JPY").format("1200"))
        assertEquals("120.00 SEK", AppCurrency.fromCode("SEK").format("120.00"))
    }

    @Test
    fun `a code in front of the amount gets a space`() {
        assertEquals("CHF 12.00", AppCurrency.fromCode("CHF").format("12.00"))
    }

    @Test
    fun `codes are read in any case`() {
        assertEquals(AppCurrency.fromCode("JPY"), AppCurrency.fromCode(" jpy "))
    }

    @Test
    fun `no code at all means the euro`() {
        assertEquals(AppCurrency.EUR, AppCurrency.fromCode(null))
        assertEquals(AppCurrency.EUR, AppCurrency.fromCode(""))
    }

    @Test
    fun `an unknown code is kept rather than replaced`() {
        // So an order in it still saves back in it, not in euros.
        assertEquals("ZZZ", AppCurrency.fromCode("ZZZ").code)
    }

    @Test
    fun `a search matches code name or symbol`() {
        val yen = AppCurrency.fromCode("JPY")
        assertTrue(yen.matches("jp"))
        assertTrue(yen.matches("yen"))
        assertTrue(yen.matches("¥"))
        assertTrue(yen.matches(""))
        assertFalse(yen.matches("euro"))
    }

    @Test
    fun `a region gives its currency`() {
        assertEquals("EUR", AppCurrency.forRegion("DE")?.code)
        assertEquals("JPY", AppCurrency.forRegion("jp")?.code)
        assertNull(AppCurrency.forRegion(null))
        assertNull(AppCurrency.forRegion("XX"))
    }
}
