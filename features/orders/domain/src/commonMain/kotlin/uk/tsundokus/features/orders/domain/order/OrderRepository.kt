package uk.tsundokus.features.orders.domain.order

import kotlinx.coroutines.flow.Flow
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState

/**
 * Offline-first source of truth: reads come from the local Room cache (reactive Flows);
 * mutations hit the network and then persist the returned entity locally.
 */
interface OrderRepository {
    fun getOrders(): Flow<List<Order>>

    fun getOrderById(id: String): Flow<Order?>

    suspend fun fetchOrders(): EmptyResult<DataError.Remote>

    suspend fun createOrder(order: Order): Result<Order, DataError.Remote>

    suspend fun updateOrder(order: Order): Result<Order, DataError.Remote>

    suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote>

    /**
     * Saves every order in [orders] as given, in one go: for acting on a selection, and for undoing
     * that by saving the orders as they were.
     */
    suspend fun updateOrders(orders: List<Order>): EmptyResult<DataError.Remote>

    suspend fun deleteOrders(ids: Collection<String>): EmptyResult<DataError.Remote>

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
