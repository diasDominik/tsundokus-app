package uk.tsundokus.features.orders.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class IsbnTest {
    @Test
    fun `a valid ISBN-13 is kept as its digits`() {
        assertEquals("9784088820453", Isbn.normalize("9784088820453"))
    }

    @Test
    fun `hyphens and spaces are dropped`() {
        assertEquals("9784088820453", Isbn.normalize("978-4-08-882045-3"))
        assertEquals("9784088820453", Isbn.normalize("978 4 08 882045 3"))
    }

    @Test
    fun `an ISBN-10 is converted to its ISBN-13`() {
        assertEquals("9784088820453", Isbn.normalize("4-08-882045-2"))
    }

    @Test
    fun `an ISBN-10 with an X check digit is accepted`() {
        assertEquals("9780439420891", Isbn.normalize("043942089X"))
        assertEquals("9780439420891", Isbn.normalize("043942089x"))
    }

    @Test
    fun `a wrong check digit is rejected`() {
        assertNull(Isbn.normalize("9784088820454"))
        assertNull(Isbn.normalize("4088820451"))
    }

    @Test
    fun `the price barcode on a Japanese book is not an ISBN`() {
        // A valid EAN-13 in its own right, which is why the 978/979 prefix has to be checked.
        assertNull(Isbn.normalize("1920979004405"))
    }

    @Test
    fun `other lengths and characters are rejected`() {
        assertNull(Isbn.normalize(""))
        assertNull(Isbn.normalize("978408882045"))
        assertNull(Isbn.normalize("97840888204ab"))
    }

    @Test
    fun `sanitising keeps digits and X only and caps at thirteen`() {
        assertEquals("9784088820453", Isbn.sanitize("978-4-08-882045-3"))
        assertEquals("043942089X", Isbn.sanitize("0-439-42089-x"))
        assertEquals("9784088820453", Isbn.sanitize("97840888204531234"))
        assertEquals("", Isbn.sanitize("abc"))
    }
}
