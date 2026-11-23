package uk.tsundokus.features.orders.presentation.scanner

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val ISBN = "9784088820453"
private const val OTHER_ISBN = "9780439420891"

class IsbnTextSightingsTest {
    @Test
    fun `an ISBN is taken once a second frame reads the same`() {
        val sut = IsbnTextSightings(accept = { true })

        assertNull(sut.onFrame("ISBN 978-4-08-882045-3"))
        assertEquals(ISBN, sut.onFrame("ISBN 978-4-08-882045-3"))
    }

    @Test
    fun `a frame reading something else starts over`() {
        val sut = IsbnTextSightings(accept = { true })

        sut.onFrame(ISBN)
        assertNull(sut.onFrame(OTHER_ISBN))
        assertNull(sut.onFrame("blurred"))
        assertNull(sut.onFrame(ISBN))
        assertEquals(ISBN, sut.onFrame(ISBN))
    }

    @Test
    fun `several ISBNs in view settle nothing`() {
        val sut = IsbnTextSightings(accept = { true })
        val page = "$ISBN\n$OTHER_ISBN"

        assertNull(sut.onFrame(page))
        assertNull(sut.onFrame(page))
    }

    @Test
    fun `an ISBN that is not accepted is passed over and the other one in view is taken`() {
        val sut = IsbnTextSightings(accept = { it != ISBN })
        val page = "$ISBN\n$OTHER_ISBN"

        assertNull(sut.onFrame(page))
        assertEquals(OTHER_ISBN, sut.onFrame(page))
    }
}
