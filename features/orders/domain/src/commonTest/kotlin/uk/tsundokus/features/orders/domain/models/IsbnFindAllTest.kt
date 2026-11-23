package uk.tsundokus.features.orders.domain.models

import kotlin.test.Test
import kotlin.test.assertEquals

/** Pulling ISBNs out of text read off a screenshot or a photo of a page. */
class IsbnFindAllTest {
    @Test
    fun `an ISBN among other text is found`() {
        val text = "Chainsaw Man 12\nISBN-13: 978-4-08-882045-3\nPublisher: Shueisha"
        assertEquals(listOf("9784088820453"), Isbn.findAll(text))
    }

    @Test
    fun `an ISBN-10 is found and given as its ISBN-13`() {
        assertEquals(listOf("9780439420891"), Isbn.findAll("ISBN 0-439-42089-X"))
    }

    @Test
    fun `the ISBN-10 and ISBN-13 of one book count once`() {
        val text = "ISBN-10: 4088820452\nISBN-13: 978-4088820453"
        assertEquals(listOf("9784088820453"), Isbn.findAll(text))
    }

    @Test
    fun `several books on an order page are all found in page order`() {
        val text =
            """
            Order #302-1234567-8912345
            Chainsaw Man 12  ISBN 9784088820453  ¥528
            Harry Potter     ISBN 043942089X     ¥1,320
            """.trimIndent()
        assertEquals(listOf("9784088820453", "9780439420891"), Isbn.findAll(text))
    }

    @Test
    fun `digits split by single spaces are joined`() {
        assertEquals(listOf("9784088820453"), Isbn.findAll("ISBN 978 4 08 882045 3"))
    }

    @Test
    fun `a number running into the ISBN does not hide it`() {
        assertEquals(listOf("9784088820453"), Isbn.findAll("Qty 1 9784088820453 1 320"))
    }

    @Test
    fun `letters misread for digits are undone`() {
        assertEquals(listOf("9784088820453"), Isbn.findAll("978-4-O8-882O45-3"))
        assertEquals(listOf("9780439420891"), Isbn.findAll("O43942O89X"))
        assertEquals(listOf("9781408855652"), Isbn.findAll("978l4O8855652"))
    }

    @Test
    fun `full-width digits and typographic hyphens are read`() {
        assertEquals(listOf("9784088820453"), Isbn.findAll("ＩＳＢＮ９７８－４－０８－８８２０４５－３"))
        assertEquals(listOf("9784088820453"), Isbn.findAll("978‐4‐08‐882045‐3"))
    }

    @Test
    fun `a number with a wrong check digit is not taken for an ISBN`() {
        assertEquals(emptyList(), Isbn.findAll("ISBN 978-4-08-882045-4"))
    }

    @Test
    fun `the price code under a Japanese ISBN is passed over`() {
        val text = "ISBN978-4-08-882045-3\nC9979 ¥480E\n1920979004405"
        assertEquals(listOf("9784088820453"), Isbn.findAll(text))
    }

    @Test
    fun `text without an ISBN gives nothing`() {
        assertEquals(emptyList(), Isbn.findAll("Thanks for your order! Total: 1,848 yen"))
        assertEquals(emptyList(), Isbn.findAll(""))
    }
}
