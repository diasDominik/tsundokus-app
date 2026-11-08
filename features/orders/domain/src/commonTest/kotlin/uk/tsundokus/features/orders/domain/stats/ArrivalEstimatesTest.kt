package uk.tsundokus.features.orders.domain.stats

import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private var nextId = 0

/** Arrived [days] after it shipped; [shipped] moves each one so their order is clear. */
private fun delivered(
    store: String,
    days: Int,
    shipped: String = "2026-09-01",
): Order =
    Order(
        id = "id-${++nextId}",
        title = "Delivered",
        store = store,
        status = OrderStatus.RECEIVED,
        shipDate = shipped,
        receivedDate = isoPlusDays(shipped, days)!!,
    )

private fun onTheWay(
    store: String = "Thalia",
    shipDate: String = "2026-10-01",
    eta: String = "",
    status: OrderStatus = OrderStatus.SHIPPED,
): Order =
    Order(
        id = "id-${++nextId}",
        title = "On the way",
        store = store,
        status = status,
        shipDate = shipDate,
        eta = eta,
    )

class ArrivalEstimatesTest {
    @Test
    fun `a store with enough deliveries is judged on its own median`() {
        val history =
            listOf(
                delivered("Thalia", 2),
                delivered("Thalia", 3),
                delivered("Thalia", 9),
                delivered("Amazon", 1),
                delivered("Amazon", 1),
                delivered("Amazon", 1),
            )

        assertEquals(
            ArrivalEstimate("2026-10-04", EstimateBasis.STORE, sample = 3),
            history.shippingTimes().estimateFor(onTheWay(store = "Thalia")),
        )
    }

    @Test
    fun `store names match whatever their case and spacing`() {
        val history = listOf(delivered("Thalia", 5), delivered("thalia", 5), delivered(" THALIA ", 5))

        assertEquals(
            EstimateBasis.STORE,
            history.shippingTimes().estimateFor(onTheWay(store = "Tha lia"))?.basis,
        )
    }

    @Test
    fun `too few from the store falls back to every delivery`() {
        val history =
            listOf(
                delivered("Thalia", 10),
                delivered("Thalia", 10),
                delivered("Amazon", 2),
                delivered("Amazon", 2),
                delivered("Amazon", 2),
            )

        assertEquals(
            ArrivalEstimate("2026-10-03", EstimateBasis.ALL_STORES, sample = 5),
            history.shippingTimes().estimateFor(onTheWay(store = "Thalia")),
        )
    }

    @Test
    fun `an order with no store is judged on every delivery`() {
        val history = listOf(delivered("Thalia", 6), delivered("Thalia", 6), delivered("Thalia", 6))

        assertEquals(EstimateBasis.ALL_STORES, history.shippingTimes().estimateFor(onTheWay(store = ""))?.basis)
    }

    @Test
    fun `too few deliveries at all falls back to the default`() {
        val history = listOf(delivered("Thalia", 10), delivered("Amazon", 10))

        assertEquals(
            ArrivalEstimate(isoPlusDays("2026-10-01", DEFAULT_SHIPPING_DAYS)!!, EstimateBasis.DEFAULT, sample = 0),
            history.shippingTimes().estimateFor(onTheWay()),
        )
        assertEquals(EstimateBasis.DEFAULT, ShippingTimes.None.estimateFor(onTheWay())?.basis)
    }

    @Test
    fun `only the latest deliveries count`() {
        // Twenty fast ones lately; before that, slow ones that no longer say how the store ships.
        val slowBefore =
            (1..10).map {
                delivered(
                    "Thalia",
                    12,
                    shipped = "2025-01-${it.toString().padStart(2, '0')}",
                )
            }
        val fastLately =
            (1..MAX_DELIVERIES_FOR_ESTIMATE).map {
                delivered("Thalia", 2, shipped = "2026-0${it % 9 + 1}-01")
            }

        val estimate = (slowBefore + fastLately).shippingTimes().estimateFor(onTheWay())

        assertEquals(ArrivalEstimate("2026-10-03", EstimateBasis.STORE, MAX_DELIVERIES_FOR_ESTIMATE), estimate)
    }

    @Test
    fun `deliveries missing a date or dated backwards are left out`() {
        val history =
            listOf(
                delivered("Thalia", 3),
                delivered("Thalia", 3),
                delivered("Thalia", 3).copy(shipDate = ""),
                delivered("Thalia", -2),
                onTheWay(store = "Thalia"),
            )

        assertEquals(EstimateBasis.DEFAULT, history.shippingTimes().estimateFor(onTheWay())?.basis)
    }

    @Test
    fun `the user's own ETA always wins`() {
        val history = listOf(delivered("Thalia", 3), delivered("Thalia", 3), delivered("Thalia", 3))

        assertNull(history.shippingTimes().estimateFor(onTheWay(eta = "2026-10-20")))
    }

    @Test
    fun `only shipped orders with a ship date get an estimate`() {
        val times = listOf(delivered("Thalia", 3), delivered("Thalia", 3), delivered("Thalia", 3)).shippingTimes()

        assertNull(times.estimateFor(onTheWay(shipDate = "")))
        assertNull(times.estimateFor(onTheWay(status = OrderStatus.ORDERED)))
        assertNull(times.estimateFor(onTheWay(status = OrderStatus.DELAYED)))
        assertNull(times.estimateFor(onTheWay(status = OrderStatus.RECEIVED)))
    }
}
