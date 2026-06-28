package uk.tsundokus.features.orders.data.order

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.asEmptyResult
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.orders.data.mappers.toDomain
import uk.tsundokus.features.orders.data.mappers.toEntity
import uk.tsundokus.features.orders.database.TsundokuDatabase
import uk.tsundokus.features.orders.database.entities.OrderEntity
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.order.OrderService

@Single(binds = [OrderRepository::class])
class OfflineFirstOrderRepository(
    private val orderService: OrderService,
    private val database: TsundokuDatabase,
) : OrderRepository {
    override fun getOrders(): Flow<List<Order>> =
        database.orderDao
            .getOrders()
            .map { orders -> orders.map(OrderEntity::toDomain) }

    override fun getOrderById(id: String): Flow<Order?> =
        database.orderDao
            .getOrderById(id)
            .map { it?.toDomain() }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> =
        orderService
            .getOrders()
            .onSuccess { orders ->
                database.orderDao.replaceAll(orders.map(Order::toEntity))
            }.asEmptyResult()

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> =
        orderService
            .createOrder(order)
            .onSuccess { database.orderDao.upsert(it.toEntity()) }

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> =
        orderService
            .updateOrder(order)
            .onSuccess { database.orderDao.upsert(it.toEntity()) }

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> =
        orderService
            .deleteOrder(id)
            .onSuccess { database.orderDao.deleteById(id) }

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> =
        orderService
            .setStatus(id, status)
            .onSuccess { database.orderDao.upsert(it.toEntity()) }

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> =
        orderService
            .reportDelay(id, delayedTo)
            .onSuccess { database.orderDao.upsert(it.toEntity()) }

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> =
        orderService
            .setReadState(id, readState)
            .onSuccess { database.orderDao.upsert(it.toEntity()) }
}
