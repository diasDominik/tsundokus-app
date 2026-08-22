package uk.tsundokus.features.orders.domain.order

import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order

/**
 * Remote API for orders, in offline-outbox terms: writes collapse to a single idempotent
 * [upsertOrder] (create or edit, keyed on the client-minted id) plus [deleteOrder]; reads come from
 * [syncOrders] (delta since a cursor). Implemented in :features:orders:data by KtorOrderService.
 */
interface OrderService {
    /** Idempotent create-or-update on the order's (client-generated) id. */
    suspend fun upsertOrder(order: Order): Result<Order, DataError.Remote>

    suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote>

    /** Delta pull: [since] is the last cursor (null = full snapshot). */
    suspend fun syncOrders(since: Long?): Result<OrderSync, DataError.Remote>
}
