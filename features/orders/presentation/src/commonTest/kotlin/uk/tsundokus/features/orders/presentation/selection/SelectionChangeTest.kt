package uk.tsundokus.features.orders.presentation.selection

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val TODAY = "2026-10-03"

private fun order(
    status: OrderStatus = OrderStatus.ORDERED,
    readState: ReadState = ReadState.WANT,
    shipDate: String = "",
    receivedDate: String = "",
) = Order(
    id = "a",
    title = "Berserk",
    status = status,
    readState = readState,
    shipDate = shipDate,
    receivedDate = receivedDate,
)

private val receive = SelectionChange.Status(OrderStatus.RECEIVED)
private val ship = SelectionChange.Status(OrderStatus.SHIPPED)
private val cancel = SelectionChange.Status(OrderStatus.CANCELLED)

class SelectionChangeTest {
    @Test
    fun `receiving fills in today when no date is set`() {
        val received = receive.applyTo(order(status = OrderStatus.SHIPPED), TODAY)

        assertEquals(OrderStatus.RECEIVED, received?.status)
        assertEquals(TODAY, received?.receivedDate)
    }

    @Test
    fun `receiving keeps a date already set`() {
        val received = receive.applyTo(order(receivedDate = "2026-09-30"), TODAY)

        assertEquals("2026-09-30", received?.receivedDate)
    }

    @Test
    fun `shipping fills in today when no date is set`() {
        assertEquals(TODAY, ship.applyTo(order(), TODAY)?.shipDate)
        assertEquals("2026-09-01", ship.applyTo(order(shipDate = "2026-09-01"), TODAY)?.shipDate)
    }

    @Test
    fun `an order already in the state is left alone`() {
        assertNull(receive.applyTo(order(status = OrderStatus.RECEIVED), TODAY))
        assertNull(SelectionChange.Reading(ReadState.READ).applyTo(order(readState = ReadState.READ), TODAY))
    }

    @Test
    fun `a cancelled order is never received`() {
        assertNull(receive.applyTo(order(status = OrderStatus.CANCELLED), TODAY))
    }

    @Test
    fun `a received order never goes back in transit or gets cancelled`() {
        assertNull(ship.applyTo(order(status = OrderStatus.RECEIVED), TODAY))
        assertNull(cancel.applyTo(order(status = OrderStatus.RECEIVED), TODAY))
    }

    @Test
    fun `an open order can be cancelled`() {
        assertEquals(OrderStatus.CANCELLED, cancel.applyTo(order(status = OrderStatus.DELAYED), TODAY)?.status)
    }

    @Test
    fun `a reading change only touches the reading state`() {
        val before = order(status = OrderStatus.RECEIVED, receivedDate = "2026-09-30")

        assertEquals(
            before.copy(readState = ReadState.READING),
            SelectionChange.Reading(ReadState.READING).applyTo(before, TODAY),
        )
    }
}
