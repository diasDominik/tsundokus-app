package uk.tsundokus.features.orders.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

private const val ISBN = "9784088820453"
private const val OTHER_ISBN = "9780439420891"

private fun order(
    id: String,
    isbn: String = "",
    status: OrderStatus = OrderStatus.ORDERED,
    title: String = id,
    createdAt: Long = 0L,
) = Order(id = id, title = title, isbn = isbn, status = status, createdAt = createdAt)

class IsbnMatchTest {
    @Test
    fun `the one open order with the ISBN is receivable`() {
        val shipped = order("a", ISBN, OrderStatus.SHIPPED)
        val match = IsbnMatch.of(ISBN, listOf(shipped, order("b", OTHER_ISBN)))

        assertEquals(IsbnMatch.Receivable(shipped), match)
    }

    @Test
    fun `a delayed order is still open`() {
        val delayed = order("a", ISBN, OrderStatus.DELAYED)

        assertEquals(IsbnMatch.Receivable(delayed), IsbnMatch.of(ISBN, listOf(delayed)))
    }

    @Test
    fun `a received copy does not hide an open reorder`() {
        val received = order("a", ISBN, OrderStatus.RECEIVED)
        val reorder = order("b", ISBN, OrderStatus.ORDERED)

        assertEquals(IsbnMatch.Receivable(reorder), IsbnMatch.of(ISBN, listOf(received, reorder)))
    }

    @Test
    fun `two open orders with the ISBN are ambiguous oldest first`() {
        val newer = order("a", ISBN, createdAt = 2L)
        val older = order("b", ISBN, createdAt = 1L)

        assertEquals(IsbnMatch.Ambiguous(listOf(older, newer)), IsbnMatch.of(ISBN, listOf(newer, older)))
    }

    @Test
    fun `only closed orders with the ISBN report the latest as already received`() {
        val cancelled = order("a", ISBN, OrderStatus.CANCELLED, createdAt = 1L)
        val received = order("b", ISBN, OrderStatus.RECEIVED, createdAt = 2L)

        assertEquals(IsbnMatch.AlreadyReceived(received), IsbnMatch.of(ISBN, listOf(cancelled, received)))
    }

    @Test
    fun `no match offers the open orders without an ISBN by title`() {
        val zebra = order("z", title = "Zebra")
        val apple = order("a", title = "Apple", status = OrderStatus.SHIPPED)
        val closed = order("c", status = OrderStatus.RECEIVED)
        val otherIsbn = order("o", OTHER_ISBN)

        assertEquals(
            IsbnMatch.NoMatch(listOf(apple, zebra)),
            IsbnMatch.of(ISBN, listOf(zebra, apple, closed, otherIsbn)),
        )
    }
}
