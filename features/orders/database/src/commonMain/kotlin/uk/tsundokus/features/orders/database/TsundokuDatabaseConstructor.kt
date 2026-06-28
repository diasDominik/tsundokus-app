package uk.tsundokus.features.orders.database

import androidx.room3.RoomDatabaseConstructor

@Suppress("NO_ACTUAL_FOR_EXPECT")
expect object TsundokuDatabaseConstructor : RoomDatabaseConstructor<TsundokuDatabase> {
    override fun initialize(): TsundokuDatabase
}
