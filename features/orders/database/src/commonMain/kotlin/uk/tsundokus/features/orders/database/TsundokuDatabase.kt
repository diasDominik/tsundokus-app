package uk.tsundokus.features.orders.database

import androidx.room3.AutoMigration
import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import uk.tsundokus.features.orders.database.dao.OrderDao
import uk.tsundokus.features.orders.database.dao.PendingOrderOpDao
import uk.tsundokus.features.orders.database.entities.OrderEntity
import uk.tsundokus.features.orders.database.entities.PendingOrderOpEntity

@Database(
    entities = [OrderEntity::class, PendingOrderOpEntity::class],
    version = 4,
    exportSchema = true,
    // A real migration rather than the destructive fallback: dropping the tables would also drop
    // the outbox, losing any write made offline that has not reached the server yet.
    autoMigrations = [AutoMigration(from = 2, to = 3), AutoMigration(from = 3, to = 4)],
)
@ConstructedBy(TsundokuDatabaseConstructor::class)
abstract class TsundokuDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao
    abstract val pendingOrderOpDao: PendingOrderOpDao

    companion object {
        const val DATABASE_NAME = "tsundoku.db"
    }
}
