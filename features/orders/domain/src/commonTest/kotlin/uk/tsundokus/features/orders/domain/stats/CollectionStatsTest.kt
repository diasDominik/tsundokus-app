package uk.tsundokus.features.orders.domain.stats

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.dates.epochMillisFromIso
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val TODAY = "2026-10-02"

private val EUR = AppCurrency.EUR
private val JPY = AppCurrency(code = "JPY", symbol = "¥", decimals = 0, displayName = "Japanese Yen")

private var nextId = 0

private fun order(
    title: String = "One Piece ${++nextId}",
    status: OrderStatus = OrderStatus.RECEIVED,
    readState: ReadState = ReadState.WANT,
    price: Double = 10.0,
    currency: AppCurrency = EUR,
    store: String = "",
    publisher: String = "",
    orderDate: String = "2026-09-01",
    releaseDate: String = "",
    shipDate: String = "",
    eta: String = "",
    receivedDate: String = "",
    delayedTo: String = "",
    createdAt: Long = 0L,
) = Order(
    id = "id-${++nextId}",
    title = title,
    status = status,
    readState = readState,
    price = price,
    currency = currency,
    store = store,
    publisher = publisher,
    orderDate = orderDate,
    releaseDate = releaseDate,
    shipDate = shipDate,
    eta = eta,
    receivedDate = receivedDate,
    delayedTo = delayedTo,
    createdAt = createdAt,
)

private fun List<Order>.stats(
    period: StatsPeriod = StatsPeriod.LAST_12_MONTHS,
    ownCurrency: AppCurrency = EUR,
) = toCollectionStats(TODAY, period, ownCurrency)

class CollectionStatsTest {
    @Test
    fun `no orders is empty`() {
        val stats = emptyList<Order>().stats()

        assertTrue(stats.isEmpty)
        assertEquals(PileStats(0, 0, null), stats.pile)
        assertTrue(stats.spend.isEmpty())
        assertNull(stats.delivery.medianShippingDays)
    }

    @Test
    fun `cancelled orders count nowhere`() {
        val stats = listOf(order(status = OrderStatus.CANCELLED, store = "Amazon")).stats()

        assertTrue(stats.isEmpty)
        assertTrue(stats.spend.isEmpty())
        assertTrue(stats.topStores.isEmpty())
    }

    @Test
    fun `the pile is what arrived and is not read`() {
        val stats =
            listOf(
                order(readState = ReadState.WANT, receivedDate = "2026-09-02"),
                order(readState = ReadState.READING, receivedDate = "2026-01-01"),
                order(readState = ReadState.READ, receivedDate = "2025-01-01"),
                order(status = OrderStatus.SHIPPED, readState = ReadState.WANT),
            ).stats()

        assertEquals(2, stats.pile.unread)
        assertEquals(1, stats.pile.reading)
        // The book being read is older but is not waiting.
        assertEquals(30, stats.pile.oldestWaitingDays)
    }

    @Test
    fun `the collection counts owned and read volumes and series with something owned`() {
        val stats =
            listOf(
                order(title = "Berserk 1", readState = ReadState.READ),
                order(title = "Berserk 2"),
                order(title = "Vagabond 1", status = OrderStatus.ORDERED),
            ).stats()

        assertEquals(CollectionCounts(ordered = 3, owned = 2, read = 1, series = 1), stats.collection)
    }

    @Test
    fun `spend falls in the month it was ordered`() {
        val stats =
            listOf(
                order(price = 8.0, orderDate = "2026-10-01"),
                order(price = 12.0, orderDate = "2026-10-02"),
                order(price = 5.0, orderDate = "2026-03-15"),
            ).stats()

        val buckets = stats.spend.single().buckets
        assertEquals(12, buckets.size)
        assertEquals(SpendBucket(2025, 11, 0.0), buckets.first())
        assertEquals(SpendBucket(2026, 10, 20.0), buckets.last())
        assertEquals(5.0, buckets.single { it.month == 3 }.amount)
        assertEquals(25.0, stats.spend.single().total)
    }

    @Test
    fun `twelve months reach back across the new year`() {
        val stats =
            listOf(
                order(price = 7.0, orderDate = "2025-11-30"),
                order(price = 9.0, orderDate = "2025-10-31"),
            ).stats()

        val spend = stats.spend.single()
        assertEquals(7.0, spend.total)
        assertEquals(1, spend.volumes)
    }

    @Test
    fun `this year runs from january to this month`() {
        val stats =
            listOf(
                order(price = 7.0, orderDate = "2026-01-01"),
                order(price = 9.0, orderDate = "2025-12-31"),
            ).stats(StatsPeriod.THIS_YEAR)

        val spend = stats.spend.single()
        assertEquals((1..10).toList(), spend.buckets.map { it.month })
        assertEquals(7.0, spend.total)
    }

    @Test
    fun `all time has a bar per year from the first order`() {
        val stats =
            listOf(
                order(price = 7.0, orderDate = "2024-06-01"),
                order(price = 9.0, orderDate = "2026-02-01"),
            ).stats(StatsPeriod.ALL_TIME)

        val spend = stats.spend.single()
        assertEquals(
            listOf(SpendBucket(2024, null, 7.0), SpendBucket(2025, null, 0.0), SpendBucket(2026, null, 9.0)),
            spend.buckets,
        )
        assertEquals(16.0, spend.total)
    }

    @Test
    fun `an order without an order date is spent when it was added`() {
        val added = epochMillisFromIso("2026-05-20")!!
        val stats = listOf(order(price = 4.0, orderDate = "", createdAt = added)).stats()

        assertEquals(
            4.0,
            stats.spend
                .single()
                .buckets
                .single { it.month == 5 }
                .amount,
        )
    }

    @Test
    fun `currencies are never added together`() {
        val stats =
            listOf(
                order(price = 10.0, currency = EUR),
                order(price = 800.0, currency = JPY),
                order(price = 900.0, currency = JPY),
            ).stats()

        assertEquals(listOf("EUR", "JPY"), stats.spend.map { it.currency.code })
        assertEquals(10.0, stats.spend[0].total)
        assertEquals(1700.0, stats.spend[1].total)
    }

    @Test
    fun `the users own currency comes first even when used less`() {
        val stats =
            listOf(
                order(currency = JPY),
                order(currency = JPY),
                order(currency = EUR),
            ).stats(ownCurrency = EUR)

        assertEquals(
            "EUR",
            stats.spend
                .first()
                .currency.code,
        )
    }

    @Test
    fun `the average price skips volumes without a price`() {
        val stats = listOf(order(price = 10.0), order(price = 20.0), order(price = 0.0)).stats()

        assertEquals(15.0, stats.spend.single().averagePrice)
        assertEquals(3, stats.spend.single().volumes)
    }

    @Test
    fun `on the way counts every open order whenever it was placed`() {
        val stats =
            listOf(
                order(status = OrderStatus.ORDERED, price = 5.0, orderDate = "2020-01-01"),
                order(status = OrderStatus.SHIPPED, price = 6.0),
                order(status = OrderStatus.DELAYED, price = 7.0),
                order(status = OrderStatus.RECEIVED, price = 100.0),
            ).stats()

        assertEquals(18.0, stats.spend.single().onTheWay)
    }

    @Test
    fun `top stores merge spellings and keep the most used one`() {
        val stats =
            listOf(
                order(store = "Amazon"),
                order(store = "Amazon"),
                order(store = " amazon "),
                order(store = "Thalia"),
                order(store = ""),
            ).stats()

        assertEquals(listOf(RankedName("Amazon", 3), RankedName("Thalia", 1)), stats.topStores)
    }

    @Test
    fun `top lists keep five`() {
        val stats = (1..7).map { order(publisher = "Publisher $it") }.stats()

        assertEquals(5, stats.topPublishers.size)
    }

    @Test
    fun `shipping takes the median days from dispatch to arrival`() {
        val stats =
            listOf(
                order(shipDate = "2026-09-01", receivedDate = "2026-09-03"),
                order(shipDate = "2026-09-01", receivedDate = "2026-09-04"),
                order(shipDate = "2026-09-01", receivedDate = "2026-11-30"),
                order(shipDate = "", receivedDate = "2026-09-04"),
            ).stats()

        assertEquals(3, stats.delivery.medianShippingDays)
        assertEquals(3, stats.delivery.shippingSample)
    }

    @Test
    fun `an even number of deliveries rounds the median up`() {
        val stats =
            listOf(
                order(shipDate = "2026-09-01", receivedDate = "2026-09-03"),
                order(shipDate = "2026-09-01", receivedDate = "2026-09-04"),
            ).stats()

        assertEquals(3, stats.delivery.medianShippingDays)
    }

    @Test
    fun `a pre order waits from its release date`() {
        val stats =
            listOf(
                order(orderDate = "2026-01-01", releaseDate = "2026-09-01", receivedDate = "2026-09-05"),
            ).stats()

        assertEquals(4, stats.delivery.medianWaitDays)
    }

    @Test
    fun `an order placed after release waits from the order date`() {
        val stats =
            listOf(
                order(orderDate = "2026-09-10", releaseDate = "2026-01-01", receivedDate = "2026-09-12"),
            ).stats()

        assertEquals(2, stats.delivery.medianWaitDays)
    }

    @Test
    fun `stores need three known outcomes to be judged`() {
        val stats =
            listOf(
                order(store = "Thalia"),
                order(store = "Thalia"),
                order(store = "Thalia", status = OrderStatus.SHIPPED),
            ).stats()

        assertTrue(stats.storeReliability.isEmpty())
    }

    @Test
    fun `a delayed order still counts as delayed once it arrived`() {
        val stats =
            listOf(
                order(store = "Amazon", eta = "2026-09-05", delayedTo = "2026-09-15"),
                order(
                    store = "Amazon",
                    status = OrderStatus.DELAYED,
                    eta = "2026-09-10",
                    delayedTo = "2026-09-14",
                ),
                order(store = "Amazon"),
                order(store = "Amazon"),
            ).stats()

        val amazon = stats.storeReliability.single()
        assertEquals(StoreReliability("Amazon", orders = 4, delayed = 2, medianSlipDays = 7), amazon)
        assertEquals(0.5, amazon.delayRate)
    }

    @Test
    fun `a slip without an eta is counted from the release date`() {
        val stats =
            listOf(
                order(store = "Kinokuniya", releaseDate = "2026-09-01", delayedTo = "2026-09-21"),
                order(store = "Kinokuniya"),
                order(store = "Kinokuniya"),
            ).stats()

        assertEquals(20, stats.storeReliability.single().medianSlipDays)
    }

    @Test
    fun `the least reliable store comes first`() {
        val reliable = List(4) { order(store = "Thalia") }
        val late = List(3) { order(store = "Amazon", delayedTo = "2026-09-20") }

        val stats = (reliable + late).stats()

        assertEquals(listOf("Amazon", "Thalia"), stats.storeReliability.map(StoreReliability::store))
    }

    @Test
    fun `orders outside the period are not judged`() {
        val stats = List(3) { order(store = "Amazon", orderDate = "2024-01-01") }.stats()

        assertTrue(stats.storeReliability.isEmpty())
        assertTrue(stats.topStores.isEmpty())
        assertEquals(3, stats.collection.owned)
    }
}
