package uk.tsundokus.features.orders.presentation.widgets

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TODAY = "2026-10-03"

private fun order(
    id: String,
    status: OrderStatus = OrderStatus.ORDERED,
    readState: ReadState = ReadState.WANT,
    releaseDate: String = "",
    eta: String = "",
    delayedTo: String = "",
    receivedDate: String = "",
) = Order(
    id = id,
    title = "Berserk $id",
    volume = id,
    status = status,
    readState = readState,
    releaseDate = releaseDate,
    eta = eta,
    delayedTo = delayedTo,
    receivedDate = receivedDate,
)

class WidgetSnapshotTest {
    @Test
    fun `arrivals are the orders on the way soonest first`() {
        val snapshot =
            listOf(
                order("late", status = OrderStatus.DELAYED, delayedTo = "2026-10-20"),
                order("shipped", status = OrderStatus.SHIPPED, eta = "2026-10-05"),
                order("release", releaseDate = "2026-10-10"),
            ).toWidgetSnapshot(TODAY)

        assertTrue(snapshot.signedIn)
        assertEquals(listOf("shipped", "release", "late"), snapshot.arrivals.map { it.id })
        assertEquals(
            WidgetArrival("shipped", "Berserk shipped", "shipped", "2026-10-05", "SHIPPED"),
            snapshot.arrivals.first(),
        )
    }

    @Test
    fun `orders with nothing to wait for are not arrivals`() {
        val snapshot =
            listOf(
                order("released", releaseDate = "2026-09-01"),
                order("no date"),
                order("cancelled", status = OrderStatus.CANCELLED, releaseDate = "2026-10-10"),
                order("here", status = OrderStatus.RECEIVED, receivedDate = "2026-10-01"),
            ).toWidgetSnapshot(TODAY)

        assertTrue(snapshot.arrivals.isEmpty())
    }

    @Test
    fun `at most ten arrivals are kept`() {
        val snapshot = (10..21).map { order("o$it", releaseDate = "2026-11-$it") }.toWidgetSnapshot(TODAY)

        assertEquals((10..19).map { "o$it" }, snapshot.arrivals.map { it.id })
    }

    @Test
    fun `the pile is what arrived and is not read`() {
        val snapshot =
            listOf(
                order("want", status = OrderStatus.RECEIVED, receivedDate = "2026-09-03"),
                order(
                    "reading",
                    status = OrderStatus.RECEIVED,
                    readState = ReadState.READING,
                    receivedDate = "2026-01-01",
                ),
                order(
                    "read",
                    status = OrderStatus.RECEIVED,
                    readState = ReadState.READ,
                    receivedDate = "2025-01-01",
                ),
                order("on its way", status = OrderStatus.SHIPPED, eta = "2026-10-05"),
            ).toWidgetSnapshot(TODAY)

        assertEquals(2, snapshot.unread)
        assertEquals(1, snapshot.reading)
        // The volume being read is older, but it isn't waiting.
        assertEquals("2026-09-03", snapshot.oldestUnreadSince)
        assertEquals(30, snapshot.oldestWaitingDays(TODAY))
    }

    @Test
    fun `a release that has come drops off when the widget draws`() {
        val snapshot =
            listOf(
                order("release", releaseDate = "2026-10-04"),
                order("shipped", status = OrderStatus.SHIPPED, eta = "2026-10-04"),
            ).toWidgetSnapshot(TODAY)

        // Two days on: the release is out, the parcel is overdue and stays.
        assertEquals(listOf("shipped"), snapshot.arrivalsOn("2026-10-05").map { it.id })
    }

    @Test
    fun `a widget that can't read the snapshot shows the signed out one`() {
        assertFalse(WidgetSnapshot.decodeOrSignedOut(null).signedIn)
        assertFalse(WidgetSnapshot.decodeOrSignedOut("not json").signedIn)
    }

    @Test
    fun `a snapshot reads back as it was written`() {
        val snapshot =
            listOf(
                order("shipped", status = OrderStatus.SHIPPED, eta = "2026-10-05"),
            ).toWidgetSnapshot(TODAY)

        assertEquals(snapshot, WidgetSnapshot.decodeOrSignedOut(snapshot.encode()))
    }

    @Test
    fun `no unread volume has no waiting time`() {
        assertNull(emptyList<Order>().toWidgetSnapshot(TODAY).oldestWaitingDays(TODAY))
    }
}
