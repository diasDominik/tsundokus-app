package uk.tsundokus.core.domain.preferences

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [CurrencyCatalog] is app-wide, so each update here leaves every currency and region the other tests
 * rely on in place, whatever order the tests run in.
 */
class CurrencyCatalogUpdateTest {
    private val newCurrency = AppCurrency("XTS", "Ŧ", 2, "Test Currency", symbolAfterAmount = true)

    @Test
    fun `a currency added on the server is offered and formats`() {
        CurrencyCatalog.update(AppCurrency.all + newCurrency, regions = mapOf("XTS" to listOf("TS")))

        assertTrue(AppCurrency.all.any { it.code == "XTS" })
        assertEquals("12.00 Ŧ", AppCurrency.fromCode("XTS").format("12.00"))
        assertEquals("XTS", AppCurrency.forRegion("TS")?.code)
    }

    @Test
    fun `a currency the server drops is no longer offered but still formats`() {
        val yen = AppCurrency.fromCode("JPY")
        CurrencyCatalog.update(AppCurrency.all.filterNot { it.code == "JPY" }, regions = emptyMap())

        assertFalse(AppCurrency.all.any { it.code == "JPY" })
        assertEquals(yen, AppCurrency.fromCode("JPY"))

        // Offered again, for the tests that expect it in the list.
        CurrencyCatalog.update(AppCurrency.all + yen, regions = emptyMap())
    }

    @Test
    fun `an empty list from the server changes nothing`() {
        val before = AppCurrency.all

        CurrencyCatalog.update(emptyList(), regions = emptyMap())

        assertEquals(before, AppCurrency.all)
    }
}
