package uk.tsundokus.features.orders.presentation.selection

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository

/**
 * Keeps orders in memory and records every batch written or deleted, so tests can check what one
 * action sent. [failWith] makes every batch fail with that error instead.
 */
internal class RecordingOrderRepository(
    orders: List<Order>,
    var failWith: DataError.Remote? = null,
) : OrderRepository {
    val stored = MutableStateFlow(orders)
    val updatedBatches = mutableListOf<List<Order>>()
    val deletedBatches = mutableListOf<Collection<String>>()

    override fun getOrders(): Flow<List<Order>> = stored

    override fun getOrderById(id: String): Flow<Order?> = stored.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun updateOrders(orders: List<Order>): EmptyResult<DataError.Remote> {
        failWith?.let { return Result.Failure(it) }
        updatedBatches += orders
        val byId = orders.associateBy(Order::id)
        stored.value = stored.value.map { byId[it.id] ?: it }
        return Result.Success(Unit)
    }

    override suspend fun deleteOrders(ids: Collection<String>): EmptyResult<DataError.Remote> {
        failWith?.let { return Result.Failure(it) }
        deletedBatches += ids
        stored.value = stored.value.filterNot { it.id in ids }
        return Result.Success(Unit)
    }

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = error("unused")

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
