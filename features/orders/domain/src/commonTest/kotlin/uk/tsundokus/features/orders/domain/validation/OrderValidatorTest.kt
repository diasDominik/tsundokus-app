package uk.tsundokus.features.orders.domain.validation

import kotlin.test.Test
import kotlin.test.assertEquals

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
}
