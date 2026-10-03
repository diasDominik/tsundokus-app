package uk.tsundokus.features.orders.data.export

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val YEN = AppCurrency(code = "JPY", symbol = "¥", decimals = 0, displayName = "Japanese Yen")

/** The rows after the byte order mark, split on the CRLF line ends. */
private fun rowsOf(csv: String): List<String> = csv.removePrefix("\uFEFF").removeSuffix("\r\n").split("\r\n")

class OrdersCsvTest {
    @Test
    fun `the file starts with a byte order mark and the header`() {
        val csv = ordersToCsv(emptyList())

        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(listOf(CSV_COLUMNS.joinToString(",")), rowsOf(csv))
    }

    @Test
    fun `an order fills every column in header order`() {
        val order =
            Order(
                id = "id-1",
                title = "Berserk",
                volume = "41",
                author = "Kentaro Miura",
                publisher = "Panini",
                isbn = "9783741630012",
                store = "Thalia",
                price = 12.0,
                status = OrderStatus.RECEIVED,
                readState = ReadState.READ,
                orderDate = "2026-08-01",
                releaseDate = "2026-08-20",
                shipDate = "2026-08-21",
                eta = "2026-08-23",
                receivedDate = "2026-08-22",
                createdAt = 1_786_000_000_000,
            )

        val row = rowsOf(ordersToCsv(listOf(order)))[1]

        assertEquals(
            "id-1,Berserk,41,Kentaro Miura,Panini,9783741630012,Thalia,12.00,EUR,RECEIVED,READ," +
                "2026-08-01,2026-08-20,2026-08-21,2026-08-23,2026-08-22,,2026-08-06T07:06:40Z",
            row,
        )
    }

    @Test
    fun `prices use the currency's own decimals`() {
        val row =
            rowsOf(
                ordersToCsv(listOf(Order(id = "a", title = "One Piece", price = 1200.0, currency = YEN))),
            )[1]

        assertTrue(row.contains(",1200,JPY,"), row)
    }

    @Test
    fun `cells with commas quotes or line breaks are quoted`() {
        assertEquals("\"Fullmetal, Alchemist\"", escapeCsvCell("Fullmetal, Alchemist"))
        assertEquals("\"The \"\"Best\"\" Edition\"", escapeCsvCell("The \"Best\" Edition"))
        assertEquals("\"Line one\nLine two\"", escapeCsvCell("Line one\nLine two"))
        assertEquals("Plain", escapeCsvCell("Plain"))
    }

    @Test
    fun `titles in any script come through intact`() {
        val csv = ordersToCsv(listOf(Order(id = "a", title = "葬送のフリーレン 📚")))

        assertTrue(rowsOf(csv)[1].startsWith("a,葬送のフリーレン 📚,"))
    }

    @Test
    fun `text that a spreadsheet would run as a formula is made plain`() {
        assertEquals("'=HYPERLINK(\"x\")", guardFormula("=HYPERLINK(\"x\")"))
        assertEquals("'+1", guardFormula("+1"))
        assertEquals("'-Man", guardFormula("-Man"))
        assertEquals("'@sum", guardFormula("@sum"))
        assertEquals("Berserk = great", guardFormula("Berserk = great"))
        assertEquals("", guardFormula(""))
    }

    @Test
    fun `an order added before that was recorded has no added time`() {
        val row = rowsOf(ordersToCsv(listOf(Order(id = "a", title = "Akira", createdAt = 0))))[1]

        assertTrue(row.endsWith(","), row)
    }
}
