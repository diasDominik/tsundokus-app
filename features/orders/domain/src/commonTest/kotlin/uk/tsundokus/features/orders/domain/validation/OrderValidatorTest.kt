package uk.tsundokus.features.orders.domain.validation

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderValidatorTest {
    @Test
    fun `letters and symbols are rejected`() {
        assertEquals("", OrderValidator.sanitizePrice("abc"))
        assertEquals("12", OrderValidator.sanitizePrice("1a2b"))
        assertEquals("19.99", OrderValidator.sanitizePrice("£19.99"))
    }

    @Test
    fun `only the first separator is kept`() {
        assertEquals("1.99", OrderValidator.sanitizePrice("1.9.9"))
        assertEquals("1.99", OrderValidator.sanitizePrice("1,99"))
    }

    @Test
    fun `decimals are capped at two places`() {
        assertEquals("12.99", OrderValidator.sanitizePrice("12.9999"))
    }

    @Test
    fun `a leading separator is dropped`() {
        assertEquals("5", OrderValidator.sanitizePrice(".5"))
    }

    @Test
    fun `non-ascii digits are rejected because they cannot be parsed`() {
        assertEquals("", OrderValidator.sanitizePrice("٢٣"))
    }

    @Test
    fun `valid input is unchanged and still parses`() {
        assertEquals("19.99", OrderValidator.sanitizePrice("19.99"))
        assertEquals(19.99, OrderValidator.sanitizePrice("19.99").toDouble())
        assertEquals("", OrderValidator.sanitizePrice(""))
    }

    @Test
    fun `in a currency without decimals a separator is grouping`() {
        assertEquals("1200", OrderValidator.sanitizePrice("1,200", maxDecimals = 0))
        assertEquals("1200", OrderValidator.sanitizePrice("1.200", maxDecimals = 0))
        assertEquals("12000", OrderValidator.sanitizePrice("¥12,000", maxDecimals = 0))
    }

    @Test
    fun `a currency with three decimals keeps three`() {
        assertEquals("1.250", OrderValidator.sanitizePrice("1.2509", maxDecimals = 3))
    }

    @Test
    fun `an ISBN is taken by an order in any status but cancelled`() {
        val isbn = "9784088820453"
        OrderStatus.entries.forEach { status ->
            val orders = listOf(Order(id = "1", title = "t", isbn = isbn, status = status))
            assertEquals(status != OrderStatus.CANCELLED, OrderValidator.isIsbnTaken(isbn, orders))
        }
    }

    @Test
    fun `an ISBN is matched in any of its spellings`() {
        val orders = listOf(Order(id = "1", title = "t", isbn = "9784088820453"))
        assertTrue(OrderValidator.isIsbnTaken("978-4-08-882045-3", orders))
        assertTrue(OrderValidator.isIsbnTaken("4088820452", orders))
    }

    @Test
    fun `a blank or invalid ISBN is never taken`() {
        val orders = listOf(Order(id = "1", title = "t"))
        assertFalse(OrderValidator.isIsbnTaken("", orders))
        assertFalse(OrderValidator.isIsbnTaken("123", orders))
    }
}
