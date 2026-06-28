package uk.tsundokus.features.orders.database.dao

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Transaction
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow
import uk.tsundokus.features.orders.database.entities.OrderEntity

@Dao
interface OrderDao {
    @Query("SELECT * FROM orders ORDER BY createdAt DESC")
    fun getOrders(): Flow<List<OrderEntity>>

    @Query("SELECT * FROM orders WHERE id = :id")
    fun getOrderById(id: String): Flow<OrderEntity?>

    @Upsert
    suspend fun upsert(order: OrderEntity)

    @Upsert
    suspend fun upsertAll(orders: List<OrderEntity>)

    @Query("DELETE FROM orders WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM orders")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(orders: List<OrderEntity>) {
        clear()
        upsertAll(orders)
    }
}
