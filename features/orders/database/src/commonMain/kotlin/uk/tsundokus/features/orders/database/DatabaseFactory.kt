package uk.tsundokus.features.orders.database

import androidx.room3.RoomDatabase

expect class DatabaseFactory {
    fun create(): RoomDatabase.Builder<TsundokuDatabase>
}
