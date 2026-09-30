package uk.tsundokus.features.orders.domain.models

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OrderSearchTest {
    private val order =
        Order(
            id = "1",
            title = "Chainsaw Man",
            author = "Tatsuki Fujimoto",
            publisher = "Shueisha",
            isbn = "9784088820453",
        )

    @Test
    fun `a blank query finds everything`() {
        assertTrue(order.matchesQuery("  "))
    }

    @Test
    fun `title author and publisher are searched in any case`() {
        assertTrue(order.matchesQuery("chainsaw"))
        assertTrue(order.matchesQuery("FUJIMOTO"))
        assertTrue(order.matchesQuery("shueisha"))
    }

    @Test
    fun `an ISBN is found with or without hyphens`() {
        assertTrue(order.matchesQuery("9784088"))
        assertTrue(order.matchesQuery("978-4-08"))
    }

    @Test
    fun `a query of only hyphens does not match every ISBN`() {
        assertFalse(order.matchesQuery("--"))
    }
}
