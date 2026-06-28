package uk.tsundokus.features.orders.database

import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<TsundokuDatabase> {
        return Room.databaseBuilder(TsundokuDatabase.DATABASE_NAME)
    }
}
