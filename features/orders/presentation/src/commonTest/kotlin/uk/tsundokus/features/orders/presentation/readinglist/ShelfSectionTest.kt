package uk.tsundokus.features.orders.presentation.readinglist

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals

class ShelfSectionTest {
    private fun order(
        status: OrderStatus,
        readState: ReadState,
    ) = Order(id = "x", title = "Berserk 41", status = status, readState = readState)

    @Test
    fun `an unread book in hand is on the pile`() {
        assertEquals(ShelfSection.PILE, order(OrderStatus.RECEIVED, ReadState.WANT).shelfSection)
    }

    @Test
    fun `an unread book not yet received is on the way`() {
        assertEquals(ShelfSection.ON_THE_WAY, order(OrderStatus.ORDERED, ReadState.WANT).shelfSection)
    }

    @Test
    fun `reading and read win over delivery status`() {
        assertEquals(ShelfSection.READING, order(OrderStatus.ORDERED, ReadState.READING).shelfSection)
        assertEquals(ShelfSection.READ, order(OrderStatus.RECEIVED, ReadState.READ).shelfSection)
    }
}
