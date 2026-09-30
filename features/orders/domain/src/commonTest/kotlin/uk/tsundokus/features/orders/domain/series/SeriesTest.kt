package uk.tsundokus.features.orders.domain.series

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

private fun volume(
    number: Int,
    status: OrderStatus = OrderStatus.RECEIVED,
    title: String = "One Piece",
    createdAt: Long = number.toLong(),
    hasCover: Boolean = false,
) = Order(
    id = "$title-$number-$status",
    title = title,
    volume = "Vol. $number",
    status = status,
    createdAt = createdAt,
    isbn = if (hasCover) "978408$number" else "",
    hasCover = hasCover,
)

class SeriesTest {
    @Test
    fun `titles that differ only in case punctuation or volume are one series`() {
        val orders =
            listOf(
                volume(1),
                volume(2, title = "ONE PIECE"),
                volume(3, title = "One Piece!"),
                Order(id = "107", title = "ONE PIECE 107", status = OrderStatus.RECEIVED),
            )

        val series = orders.toSeries()

        assertEquals(1, series.size)
        assertEquals(listOf(1, 2, 3, 107), series.single().volumes.map(SeriesVolume::number))
    }

    @Test
    fun `the title spelled most often names the series`() {
        val series = listOf(volume(1), volume(2), volume(3, title = "ONE PIECE")).toSeries().single()

        assertEquals("One Piece", series.title)
    }

    @Test
    fun `a volume is received once any of its orders arrived`() {
        val series =
            listOf(volume(1), volume(2, OrderStatus.SHIPPED), volume(2, OrderStatus.RECEIVED, createdAt = 9))
                .toSeries()
                .single()

        assertEquals(VolumeState.RECEIVED, series.volumes.single { it.number == 2 }.state)
        assertEquals(
            2,
            series.volumes
                .single { it.number == 2 }
                .orders.size,
        )
    }

    @Test
    fun `volumes still coming are on the way`() {
        val series = listOf(volume(1), volume(2, OrderStatus.DELAYED)).toSeries().single()

        assertEquals(1, series.receivedCount)
        assertEquals(1, series.onTheWayCount)
    }

    @Test
    fun `a series shows its most pressing state`() {
        assertEquals(
            OrderStatus.DELAYED,
            listOf(
                volume(1),
                volume(2, OrderStatus.SHIPPED),
                volume(3, OrderStatus.DELAYED),
            ).toSeries().single().status,
        )
        assertEquals(
            OrderStatus.SHIPPED,
            listOf(volume(1), volume(2, OrderStatus.SHIPPED)).toSeries().single().status,
        )
        assertEquals(OrderStatus.RECEIVED, listOf(volume(1), volume(2)).toSeries().single().status)
    }

    @Test
    fun `cancelled orders are not part of the collection`() {
        val series = listOf(volume(1), volume(2, OrderStatus.CANCELLED), volume(3)).toSeries().single()

        assertEquals(listOf(2), series.missing)
    }

    @Test
    fun `gaps count from the lowest volume not from 1`() {
        val series = listOf(volume(5), volume(6), volume(8)).toSeries().single()

        assertEquals(listOf(7), series.missing)
    }

    @Test
    fun `the next volume follows the highest and keeps the user's format`() {
        val series = listOf(volume(1), volume(2)).toSeries().single()

        assertEquals(3, series.nextVolume)
        assertEquals("Vol. 3", series.nextVolumeLabel)
    }

    @Test
    fun `a bare volume number stays bare`() {
        val series =
            listOf(
                Order(id = "1", title = "Berserk", volume = "41", status = OrderStatus.RECEIVED),
            ).toSeries().single()

        assertEquals("42", series.nextVolumeLabel)
    }

    @Test
    fun `a book without a volume number is a one-shot`() {
        val series =
            listOf(
                Order(id = "1", title = "Look Back", status = OrderStatus.RECEIVED),
            ).toSeries().single()

        assertTrue(series.isOneShot)
        assertNull(series.nextVolume)
    }

    @Test
    fun `an unnumbered order in a numbered series is an extra`() {
        val artbook = Order(id = "art", title = "One Piece", status = OrderStatus.RECEIVED)
        val series = listOf(volume(1), artbook).toSeries().single()

        assertEquals(listOf(artbook), series.extras)
        assertEquals(listOf(1), series.volumes.map(SeriesVolume::number))
    }

    @Test
    fun `the cover is the highest volume that has one`() {
        val series = listOf(volume(1, hasCover = true), volume(2, hasCover = true), volume(3)).toSeries().single()

        assertEquals("9784082", series.coverIsbn)
    }

    @Test
    fun `the latest order is where the next volume's details come from`() {
        val older = volume(1, createdAt = 1).copy(store = "Amazon")
        val newer = volume(2, createdAt = 2).copy(store = "Hugendubel")

        assertEquals(
            "Hugendubel",
            listOf(older, newer)
                .toSeries()
                .single()
                .latest.store,
        )
    }

    @Test
    fun `a series is found by its title or anything in its orders`() {
        val series = listOf(volume(1).copy(author = "Eiichiro Oda", isbn = "9784088820453")).toSeries().single()

        assertTrue(series.matchesQuery("one pie"))
        assertTrue(series.matchesQuery("Oda"))
        assertTrue(series.matchesQuery("978-4-08"))
    }
}
