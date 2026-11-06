package uk.tsundokus.features.orders.domain.series

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private val JPY = AppCurrency.fromCode("JPY")

private fun order(
    createdAt: Long,
    title: String = "Naruto",
    author: String = "",
    publisher: String = "",
    store: String = "",
    price: Double = 0.0,
    currency: AppCurrency = AppCurrency.EUR,
    status: OrderStatus = OrderStatus.RECEIVED,
    isbn: String = "",
) = Order(
    id = "$title-$createdAt",
    title = title,
    author = author,
    publisher = publisher,
    store = store,
    price = price,
    currency = currency,
    status = status,
    isbn = isbn,
    createdAt = createdAt,
)

class SeriesDetailsTest {
    @Test
    fun `the newest order of the series gives the details`() {
        val orders =
            listOf(
                order(1, publisher = "Old", store = "Old shop", price = 5.0),
                order(2, publisher = "Carlsen", store = "Thalia", price = 7.5),
            )

        val details = orders.seriesDetailsFor("Naruto", "")

        assertEquals("Carlsen", details?.publisher)
        assertEquals("Thalia", details?.store)
        assertEquals(7.5, details?.price)
    }

    @Test
    fun `a field the newest order lacks comes from the one before`() {
        val orders =
            listOf(
                order(1, author = "Kishimoto", publisher = "Carlsen", price = 7.5),
                order(2, store = "Thalia"),
            )

        val details = orders.seriesDetailsFor("Naruto", "")

        assertEquals("Kishimoto", details?.author)
        assertEquals("Carlsen", details?.publisher)
        assertEquals("Thalia", details?.store)
        assertEquals(7.5, details?.price)
    }

    @Test
    fun `a volume number case and punctuation don't make another series`() {
        val orders = listOf(order(1, title = "NARUTO!, Vol. 11", publisher = "Carlsen"))

        assertEquals("Carlsen", orders.seriesDetailsFor("Naruto", "")?.publisher)
        assertEquals("Carlsen", orders.seriesDetailsFor("Naruto Vol. 12", "")?.publisher)
    }

    @Test
    fun `another series gives nothing`() {
        val orders = listOf(order(1, title = "One Piece", publisher = "Carlsen"))

        assertNull(orders.seriesDetailsFor("Naruto", ""))
    }

    @Test
    fun `a title with nothing to match on gives nothing`() {
        val orders = listOf(order(1, title = "!!!", publisher = "Carlsen"))

        assertNull(orders.seriesDetailsFor("!!!", ""))
        assertNull(orders.seriesDetailsFor("", ""))
    }

    @Test
    fun `a title ending in a number still finds the order with that very title`() {
        // With a western ISBN "451" is part of the title; without one it would read as a volume.
        val orders = listOf(order(1, title = "Fahrenheit 451", publisher = "Ballantine", isbn = "9781451673319"))

        assertEquals("Ballantine", orders.seriesDetailsFor("Fahrenheit 451", "")?.publisher)
    }

    @Test
    fun `a volume in another series with the same start is not matched`() {
        val orders = listOf(order(1, title = "Fahrenheit 451", publisher = "Ballantine", isbn = "9781451673319"))

        assertNull(orders.seriesDetailsFor("Fahrenheit 9/11", "9781451673319"))
    }

    @Test
    fun `the price keeps the currency of the order it came from`() {
        val orders =
            listOf(
                order(1, price = 700.0, currency = JPY),
                order(2, store = "Thalia", currency = AppCurrency.EUR),
            )

        val details = orders.seriesDetailsFor("Naruto", "")

        assertEquals(700.0, details?.price)
        assertEquals(JPY, details?.currency)
    }

    @Test
    fun `without any price the currency is the newest order's`() {
        val orders =
            listOf(
                order(1, currency = AppCurrency.EUR),
                order(2, currency = JPY),
            )

        val details = orders.seriesDetailsFor("Naruto", "")

        assertNull(details?.price)
        assertEquals(JPY, details?.currency)
    }

    @Test
    fun `a cancelled order only fills what the others leave open`() {
        val orders =
            listOf(
                order(1, store = "Thalia", price = 7.5),
                order(
                    2,
                    publisher = "Carlsen",
                    store = "Cancelled shop",
                    price = 99.0,
                    status = OrderStatus.CANCELLED,
                ),
            )

        val details = orders.seriesDetailsFor("Naruto", "")

        assertEquals("Thalia", details?.store)
        assertEquals(7.5, details?.price)
        assertEquals("Carlsen", details?.publisher)
    }

    @Test
    fun `a series of only cancelled orders still gives its details`() {
        val orders = listOf(order(1, publisher = "Carlsen", status = OrderStatus.CANCELLED))

        assertEquals("Carlsen", orders.seriesDetailsFor("Naruto", "")?.publisher)
    }
}
