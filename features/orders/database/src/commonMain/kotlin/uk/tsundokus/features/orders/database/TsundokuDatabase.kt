package uk.tsundokus.features.orders.database

import androidx.room3.ConstructedBy
import androidx.room3.Database
import androidx.room3.RoomDatabase
import uk.tsundokus.features.orders.database.dao.OrderDao
import uk.tsundokus.features.orders.database.entities.OrderEntity

@Database(
    entities = [OrderEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(TsundokuDatabaseConstructor::class)
abstract class TsundokuDatabase : RoomDatabase() {
    abstract val orderDao: OrderDao

    companion object {
        const val DATABASE_NAME = "tsundoku.db"
    }
}
