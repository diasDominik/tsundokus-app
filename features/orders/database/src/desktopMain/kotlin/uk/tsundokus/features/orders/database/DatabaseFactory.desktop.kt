package uk.tsundokus.features.orders.database

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

actual class DatabaseFactory {
    actual fun create(): RoomDatabase.Builder<TsundokuDatabase> {
        val databaseFile =
            File(
                System.getProperty("user.home"),
                ".tsundoku/${TsundokuDatabase.DATABASE_NAME}",
            )
        databaseFile.parentFile?.mkdirs()
        return Room.databaseBuilder(databaseFile.absolutePath)
    }
}
