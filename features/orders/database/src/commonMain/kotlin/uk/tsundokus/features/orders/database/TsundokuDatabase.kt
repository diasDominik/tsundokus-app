package uk.tsundokus.features.orders.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import uk.tsundokus.features.orders.database.dao.OrderDao
import uk.tsundokus.features.orders.database.dao.PendingOrderOpDao
import uk.tsundokus.features.orders.database.entities.OrderEntity
import uk.tsundokus.features.orders.database.entities.PendingOrderOpEntity

@Database(
    entities = [OrderEntity::class, PendingOrderOpEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(TsundokuDatabaseConstructor::class)
abstract class TsundokuDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao
    abstract val pendingOrderOpDao: PendingOrderOpDao

    companion object {
        const val DATABASE_NAME = "tsundoku.db"
    }
}
