package uk.tsundokus.features.orders.domain.order

import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/** Remote API for orders. Implemented in :features:orders:data by KtorOrderService. */
interface OrderService {
    suspend fun getOrders(): Result<List<Order>, DataError.Remote>

    suspend fun createOrder(order: Order): Result<Order, DataError.Remote>

    suspend fun updateOrder(order: Order): Result<Order, DataError.Remote>

    suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote>

    suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote>

    suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote>

    suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote>
}
