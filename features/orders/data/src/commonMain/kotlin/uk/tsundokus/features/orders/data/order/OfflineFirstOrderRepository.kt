package uk.tsundokus.features.orders.data.order

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.di.APPLICATION_SCOPE
import uk.tsundokus.core.domain.sync.LastServerContactStore
import uk.tsundokus.core.domain.sync.SyncCursorStore
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.data.mappers.toDomain
import uk.tsundokus.features.orders.data.mappers.toEntity
import uk.tsundokus.features.orders.database.TsundokuDatabase
import uk.tsundokus.features.orders.database.entities.OrderEntity
import uk.tsundokus.features.orders.database.entities.PendingOrderOpEntity
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.order.OrderService
import kotlin.time.Clock

/**
 * Offline-first with a write outbox. Reads come from the Room cache. Writes apply locally at once
 * (optimistic, marked `pendingSync`) and enqueue an outbox op, then a background sync drains the
 * outbox against the server and pulls the delta. So a write made offline survives an app kill and
 * replays when connectivity returns; nothing in the write path waits on the network.
 *
 * All mutations collapse to one idempotent UPSERT (keyed on the client-minted id) or a DELETE — the
 * whole current order state already lives locally, so replay just re-sends it.
 */
@Single(binds = [OrderRepository::class])
class OfflineFirstOrderRepository(
    private val orderService: OrderService,
    private val database: TsundokuDatabase,
    private val syncCursorStore: SyncCursorStore,
    private val lastServerContactStore: LastServerContactStore,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) : OrderRepository {
    private val orderDao get() = database.orderDao
    private val outboxDao get() = database.pendingOrderOpDao
    private val syncMutex = Mutex()

    override fun getOrders(): Flow<List<Order>> =
        orderDao.getOrders().map { orders -> orders.map(OrderEntity::toDomain) }

    override fun getOrderById(id: String): Flow<Order?> = orderDao.getOrderById(id).map { it?.toDomain() }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = sync()

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = enqueueUpsert(order)

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = enqueueUpsert(order)

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> {
        orderDao.deleteById(id)
        // Keyed by orderId, so this overwrites any still-pending UPSERT for the same order — a
        // delete of a never-synced order simply cancels its create.
        outboxDao.upsert(PendingOrderOpEntity(id, PendingOrderOpEntity.Type.DELETE.name, now()))
        triggerSync()
        return Result.Success(Unit)
    }

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> {
        // The server fills shipDate/receivedDate on the upsert; the local copy catches up on the
        // next delta pull.
        val current = localOrder(id) ?: return Result.Failure(DataError.Remote.UNKNOWN)
        return enqueueUpsert(current.copy(status = status))
    }

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> {
        val current = localOrder(id) ?: return Result.Failure(DataError.Remote.UNKNOWN)
        return enqueueUpsert(current.copy(status = OrderStatus.DELAYED, delayedTo = delayedTo))
    }

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> {
        val current = localOrder(id) ?: return Result.Failure(DataError.Remote.UNKNOWN)
        return enqueueUpsert(current.copy(readState = readState))
    }

    private suspend fun localOrder(id: String): Order? = orderDao.getOrderByIdOnce(id)?.toDomain()

    private suspend fun enqueueUpsert(order: Order): Result<Order, DataError.Remote> {
        orderDao.upsert(order.toEntity(pendingSync = true))
        outboxDao.upsert(PendingOrderOpEntity(order.id, PendingOrderOpEntity.Type.UPSERT.name, now()))
        triggerSync()
        return Result.Success(order)
    }

    /** Fire-and-forget push: promptly drains when online, silently stays queued when offline. */
    private fun triggerSync() {
        appScope.launch { sync() }
    }

    /** Drain the outbox, then pull the server delta. Serialized so concurrent triggers don't race. */
    private suspend fun sync(): EmptyResult<DataError.Remote> =
        syncMutex.withLock {
            drainOutbox()?.let { return@withLock Result.Failure(it) }
            pullDelta()
        }

    /** Replays queued writes oldest-first. Returns the first error (keeping the rest for retry). */
    private suspend fun drainOutbox(): DataError.Remote? {
        outboxDao.getAll().forEach { op ->
            when (op.type) {
                PendingOrderOpEntity.Type.UPSERT.name -> {
                    val local = orderDao.getOrderByIdOnce(op.orderId)
                    if (local == null) {
                        // Order was deleted after the upsert was queued; the delete op supersedes it.
                        outboxDao.deleteByOrderId(op.orderId)
                        return@forEach
                    }
                    when (val result = orderService.upsertOrder(local.toDomain())) {
                        is Result.Success -> {
                            orderDao.upsert(result.data.toEntity(pendingSync = false))
                            outboxDao.deleteByOrderId(op.orderId)
                        }

                        is Result.Failure -> {
                            return result.error
                        }
                    }
                }

                PendingOrderOpEntity.Type.DELETE.name -> {
                    when (val result = orderService.deleteOrder(op.orderId)) {
                        is Result.Success -> {
                            outboxDao.deleteByOrderId(op.orderId)
                        }

                        is Result.Failure -> {
                            // Already gone server-side is a success for a delete.
                            if (result.error == DataError.Remote.NOT_FOUND) {
                                outboxDao.deleteByOrderId(op.orderId)
                            } else {
                                return result.error
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    private suspend fun pullDelta(): EmptyResult<DataError.Remote> =
        when (val result = orderService.syncOrders(syncCursorStore.get())) {
            is Result.Success -> {
                val delta = result.data
                // Never clobber a row that still has an unsynced local write — its outbox op will
                // push the local truth on the next drain.
                val pendingIds = outboxDao.getAll().map { it.orderId }.toSet()
                delta.changed
                    .filter { it.id !in pendingIds }
                    .forEach { orderDao.upsert(it.toEntity(pendingSync = false)) }
                delta.deletedIds
                    .filter { it !in pendingIds }
                    .forEach { orderDao.deleteById(it) }
                syncCursorStore.set(delta.serverTime)
                lastServerContactStore.record(delta.serverTime)
                Result.Success(Unit)
            }

            is Result.Failure -> {
                Result.Failure(result.error)
            }
        }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
