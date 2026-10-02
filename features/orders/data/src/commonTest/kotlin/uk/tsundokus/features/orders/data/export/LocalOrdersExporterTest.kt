package uk.tsundokus.features.orders.data.export

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.export.ExportFormat
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class FixedOrders(
    private val orders: List<Order>,
) : OrderRepository {
    override fun getOrders(): Flow<List<Order>> = flowOf(orders)

    override fun getOrderById(id: String): Flow<Order?> = error("unused")

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun updateOrders(orders: List<Order>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun deleteOrders(ids: Collection<String>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> = error("unused")

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> = error("unused")

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> = error("unused")
}

private val orders =
    listOf(
        Order(id = "newer", title = "Berserk 42", createdAt = 2),
        Order(id = "cancelled", title = "Akira 1", status = OrderStatus.CANCELLED, createdAt = 3),
        Order(id = "older", title = "Berserk 41", createdAt = 1),
    )

class LocalOrdersExporterTest {
    private val sut = LocalOrdersExporter(FixedOrders(orders))

    @Test
    fun `every order goes in oldest first cancelled ones too`() =
        runTest {
            val csv = sut.export(ExportFormat.CSV).bytes.decodeToString()

            val ids =
                csv
                    .removeSuffix("\r\n")
                    .split("\r\n")
                    .drop(1)
                    .map { it.substringBefore(',') }
            assertEquals(listOf("older", "newer", "cancelled"), ids)
        }

    @Test
    fun `the file is named for the app and the day`() =
        runTest {
            val file = sut.export(ExportFormat.JSON)

            assertTrue(Regex("tsundoku-orders-\\d{4}-\\d{2}-\\d{2}\\.json").matches(file.fileName), file.fileName)
            assertEquals("application/json", file.format.mimeType)
        }
}
