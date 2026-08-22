package uk.tsundokus.features.orders.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import uk.tsundokus.features.orders.database.entities.PendingOrderOpEntity

@Dao
interface PendingOrderOpDao {
    /** Coalesces to one row per order: a fresh edit replaces the prior pending write for that id. */
    @Upsert
    suspend fun upsert(op: PendingOrderOpEntity)

    @Query("SELECT * FROM pending_order_ops ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingOrderOpEntity>

    @Query("SELECT COUNT(*) FROM pending_order_ops")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_order_ops WHERE orderId = :orderId")
    suspend fun deleteByOrderId(orderId: String)

    @Query("DELETE FROM pending_order_ops")
    suspend fun clear()
}
