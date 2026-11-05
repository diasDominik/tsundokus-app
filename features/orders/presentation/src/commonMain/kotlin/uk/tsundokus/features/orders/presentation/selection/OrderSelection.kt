package uk.tsundokus.features.orders.presentation.selection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderChange
import uk.tsundokus.features.orders.domain.order.OrderRepository

/**
 * The orders picked on a list, and acting on them together. [picked] is null while the list is not
 * in selection mode.
 *
 * The last change is remembered as the orders were before it, so it can be undone by saving them
 * back. Deleting is not undoable: the list asks before it deletes.
 */
class OrderSelection(
    private val orderRepository: OrderRepository,
) {
    private val pickedIds = MutableStateFlow<Set<String>?>(null)
    val picked: StateFlow<Set<String>?> = pickedIds.asStateFlow()

    private var beforeLastChange: List<Order> = emptyList()

    /** Picks or unpicks [id], starting selection mode if needed; unpicking the last one ends it. */
    fun toggle(id: String) {
        val current = pickedIds.value.orEmpty()
        val next = if (id in current) current - id else current + id
        pickedIds.value = next.ifEmpty { null }
    }

    fun selectAll(ids: Collection<String>) {
        pickedIds.value = pickedIds.value.orEmpty() + ids
    }

    fun clear() {
        pickedIds.value = null
    }

    /**
     * Makes [change] to the picked orders among [orders] and leaves selection mode. Returns how many
     * changed; 0 means none of them could, and the selection stays so another action can be tried.
     */
    suspend fun apply(
        change: OrderChange,
        orders: List<Order>,
        today: String,
    ): Result<Int, DataError.Remote> {
        val picked = pickedIds.value ?: return Result.Success(0)
        val changes =
            orders
                .filter { it.id in picked }
                .mapNotNull { order -> change.applyTo(order, today)?.let { order to it } }
        if (changes.isEmpty()) return Result.Success(0)
        return orderRepository
            .updateOrders(changes.map { (_, changed) -> changed })
            .onSuccess {
                beforeLastChange = changes.map { (before, _) -> before }
                clear()
            }.map { changes.size }
    }

    /** Deletes the picked orders among [orders] and leaves selection mode; returns how many. */
    suspend fun delete(orders: List<Order>): Result<Int, DataError.Remote> {
        val picked = pickedIds.value ?: return Result.Success(0)
        val ids = orders.map(Order::id).filter { it in picked }
        return orderRepository
            .deleteOrders(ids)
            .onSuccess {
                beforeLastChange = emptyList()
                clear()
            }.map { ids.size }
    }

    /** Saves the orders changed last back as they were. Does nothing once undone, or after a delete. */
    suspend fun undo(): EmptyResult<DataError.Remote> {
        val before = beforeLastChange
        if (before.isEmpty()) return Result.Success(Unit)
        beforeLastChange = emptyList()
        return orderRepository.updateOrders(before)
    }
}
