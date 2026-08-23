package uk.tsundokus.features.orders.domain.validation

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderDateValidationTest {
    @Test
    fun `a date before the floor is rejected`() {
        assertTrue(OrderValidator.isBefore("2026-01-01", floor = "2026-06-10"))
    }

    @Test
    fun `the same day is not before the floor`() {
        assertFalse(OrderValidator.isBefore("2026-06-10", floor = "2026-06-10"))
    }

    @Test
    fun `a later date is not before the floor`() {
        assertFalse(OrderValidator.isBefore("2026-12-31", floor = "2026-06-10"))
    }

    @Test
    fun `a blank date is unknown, never a violation`() {
        assertFalse(OrderValidator.isBefore("", floor = "2026-06-10"))
        assertFalse(OrderValidator.isBefore("2026-01-01", floor = ""))
    }

    @Test
    fun `comparison holds across month and day boundaries`() {
        // Guards the string-comparison shortcut: zero padding is what makes "09" < "10" correct.
        assertTrue(OrderValidator.isBefore("2026-09-30", floor = "2026-10-01"))
        assertTrue(OrderValidator.isBefore("2026-06-09", floor = "2026-06-10"))
        assertFalse(OrderValidator.isBefore("2026-10-01", floor = "2026-09-30"))
    }
}
