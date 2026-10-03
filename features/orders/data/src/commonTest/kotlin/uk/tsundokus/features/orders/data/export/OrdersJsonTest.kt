package uk.tsundokus.features.orders.data.export

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import kotlin.test.Test
import kotlin.test.assertEquals

private const val EXPORTED_AT = 1_791_000_000_000L

private val order =
    Order(
        id = "id-1",
        title = "Berserk",
        volume = "41",
        price = 12.99,
        status = OrderStatus.SHIPPED,
        readState = ReadState.WANT,
        orderDate = "2026-08-01",
        createdAt = 1_786_000_000_000,
    )

class OrdersJsonTest {
    @Test
    fun `the document names its format and version`() {
        val document = Json.parseToJsonElement(ordersToJson(listOf(order), EXPORTED_AT)).jsonObject

        assertEquals("tsundoku-orders", document.getValue("format").jsonPrimitive.content)
        assertEquals(1, document.getValue("version").jsonPrimitive.int)
        assertEquals("2026-10-03T04:00:00Z", document.getValue("exportedAt").jsonPrimitive.content)
        assertEquals(1, document.getValue("orders").jsonArray.size)
    }

    @Test
    fun `unset text and dates are null`() {
        val exported =
            Json
                .parseToJsonElement(ordersToJson(listOf(order), EXPORTED_AT))
                .jsonObject
                .getValue("orders")
                .jsonArray
                .single()
                .jsonObject

        assertEquals(JsonNull, exported.getValue("author"))
        assertEquals(JsonNull, exported.getValue("receivedDate"))
        assertEquals("2026-08-01", exported.getValue("orderDate").jsonPrimitive.content)
    }

    @Test
    fun `the file reads back into the same values`() {
        val document = exportJson.decodeFromString<OrdersExportDocument>(ordersToJson(listOf(order), EXPORTED_AT))

        assertEquals(
            ExportedOrder(
                id = "id-1",
                title = "Berserk",
                volume = "41",
                author = null,
                publisher = null,
                isbn = null,
                store = null,
                price = 12.99,
                currency = "EUR",
                status = "SHIPPED",
                readState = "WANT",
                orderDate = "2026-08-01",
                releaseDate = null,
                shipDate = null,
                eta = null,
                receivedDate = null,
                delayedTo = null,
                addedAt = "2026-08-06T07:06:40Z",
            ),
            document.orders.single(),
        )
    }
}
