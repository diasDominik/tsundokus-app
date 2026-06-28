package uk.tsundokus.features.orders.database

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

actual class DatabaseFactory(
    private val context: Context,
) {
    actual fun create(): RoomDatabase.Builder<TsundokuDatabase> {
        val databaseFile = context.getDatabasePath(TsundokuDatabase.DATABASE_NAME)
        return Room.databaseBuilder(
            context.applicationContext,
            databaseFile.absolutePath,
        )
    }
}
