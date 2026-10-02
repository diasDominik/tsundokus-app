package uk.tsundokus.features.orders.presentation.components

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.formatAmount
import kotlin.test.Test
import kotlin.test.assertEquals

class AmountFormattingTest {
    @Test
    fun `amounts show the currency's own decimals`() {
        assertEquals("19.99", formatAmount(19.99, 2))
        assertEquals("1200", formatAmount(1200.0, 0))
        assertEquals("1.250", formatAmount(1.25, 3))
    }

    @Test
    fun `amounts are rounded to those decimals`() {
        assertEquals("1201", formatAmount(1200.6, 0))
        assertEquals("0.10", formatAmount(0.099, 2))
    }

    @Test
    fun `a price label puts the symbol where the currency does`() {
        assertEquals(
            "19.99 €",
            priceLabel(Order(id = "1", title = "t", price = 19.99, currency = AppCurrency.EUR)),
        )
        assertEquals(
            "¥1200",
            priceLabel(Order(id = "1", title = "t", price = 1200.0, currency = AppCurrency.fromCode("JPY"))),
        )
        assertEquals(
            "120.00 SEK",
            priceLabel(Order(id = "1", title = "t", price = 120.0, currency = AppCurrency.fromCode("SEK"))),
        )
    }
}
