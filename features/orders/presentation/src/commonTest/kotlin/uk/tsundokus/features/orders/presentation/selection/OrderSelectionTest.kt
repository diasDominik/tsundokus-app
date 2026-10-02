package uk.tsundokus.features.orders.presentation.selection

import kotlinx.coroutines.test.runTest
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TODAY = "2026-10-03"

private val ordered = Order(id = "ordered", title = "Berserk 41", status = OrderStatus.ORDERED)
private val shipped = Order(id = "shipped", title = "Berserk 42", status = OrderStatus.SHIPPED)
private val received = Order(id = "received", title = "Berserk 40", status = OrderStatus.RECEIVED)
private val cancelled = Order(id = "cancelled", title = "Akira 1", status = OrderStatus.CANCELLED)

private val allOrders = listOf(ordered, shipped, received, cancelled)

private val receive = SelectionChange.Status(OrderStatus.RECEIVED)

class OrderSelectionTest {
    private val repository = RecordingOrderRepository(allOrders)
    private val sut = OrderSelection(repository)

    @Test
    fun `nothing is picked until picking starts`() {
        assertNull(sut.picked.value)

        sut.start()

        assertEquals(emptySet(), sut.picked.value)
    }

    @Test
    fun `toggling picks and unpicks and the last unpick stops picking`() {
        sut.toggle("ordered")
        sut.toggle("shipped")
        assertEquals(setOf("ordered", "shipped"), sut.picked.value)

        sut.toggle("ordered")
        assertEquals(setOf("shipped"), sut.picked.value)

        sut.toggle("shipped")
        assertNull(sut.picked.value)
    }

    @Test
    fun `picking all adds to what is already picked`() {
        sut.toggle("cancelled")

        sut.selectAll(listOf("ordered", "shipped"))

        assertEquals(setOf("cancelled", "ordered", "shipped"), sut.picked.value)
    }

    @Test
    fun `a change writes only the picked orders it applies to in one batch`() =
        runTest {
            sut.selectAll(listOf("ordered", "received", "cancelled"))

            val result = sut.apply(receive, allOrders, TODAY)

            assertEquals(Result.Success(1), result)
            assertEquals(1, repository.updatedBatches.size)
            val written = repository.updatedBatches.single().single()
            assertEquals("ordered", written.id)
            assertEquals(OrderStatus.RECEIVED, written.status)
            assertEquals(TODAY, written.receivedDate)
            assertNull(sut.picked.value)
        }

    @Test
    fun `a change none of the picked orders can take writes nothing and keeps them picked`() =
        runTest {
            sut.selectAll(listOf("received", "cancelled"))

            val result = sut.apply(receive, allOrders, TODAY)

            assertEquals(Result.Success(0), result)
            assertTrue(repository.updatedBatches.isEmpty())
            assertEquals(setOf("received", "cancelled"), sut.picked.value)
        }

    @Test
    fun `undo saves the changed orders back as they were`() =
        runTest {
            sut.selectAll(listOf("ordered", "shipped"))
            sut.apply(SelectionChange.Reading(ReadState.READ), allOrders, TODAY)

            sut.undo()

            assertEquals(listOf(ordered, shipped), repository.updatedBatches.last())
            assertEquals(allOrders, repository.stored.value)
        }

    @Test
    fun `undo only undoes once`() =
        runTest {
            sut.toggle("ordered")
            sut.apply(receive, allOrders, TODAY)
            sut.undo()

            sut.undo()

            assertEquals(2, repository.updatedBatches.size)
        }

    @Test
    fun `deleting removes the picked orders and cannot be undone`() =
        runTest {
            sut.selectAll(listOf("ordered", "cancelled"))

            val result = sut.delete(allOrders)
            sut.undo()

            assertEquals(Result.Success(2), result)
            assertEquals(listOf(setOf("ordered", "cancelled")), repository.deletedBatches.map { it.toSet() })
            assertTrue(repository.updatedBatches.isEmpty())
            assertNull(sut.picked.value)
        }

    @Test
    fun `an order gone since it was picked is not sent`() =
        runTest {
            sut.selectAll(listOf("ordered", "vanished"))

            sut.delete(allOrders)

            assertEquals(listOf("ordered"), repository.deletedBatches.single().toList())
        }

    @Test
    fun `a failed change keeps the orders picked and leaves nothing to undo`() =
        runTest {
            repository.failWith = DataError.Remote.UNKNOWN
            sut.toggle("ordered")

            val result = sut.apply(receive, allOrders, TODAY)
            repository.failWith = null
            sut.undo()

            assertIs<Result.Failure<DataError.Remote>>(result)
            assertEquals(setOf("ordered"), sut.picked.value)
            assertTrue(repository.updatedBatches.isEmpty())
        }
}
