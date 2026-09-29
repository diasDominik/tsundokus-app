package uk.tsundokus.features.orders.database.entities

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val volume: String,
    val store: String,
    val price: Double,
    val currency: String,
    val status: String,
    val readState: String,
    val orderDate: String,
    val releaseDate: String,
    val shipDate: String,
    val eta: String,
    val receivedDate: String,
    val delayedTo: String,
    // ISBN-13 digits; blank on orders made before ISBNs were recorded. The column default is what
    // lets the 2 -> 3 auto-migration add it to existing rows.
    @ColumnInfo(defaultValue = "")
    val isbn: String = "",
    val createdAt: Long,
    // True while a local write for this row hasn't been confirmed by the server. Set by optimistic
    // writes, cleared when delta sync applies the server's copy. Drives the "not synced" indicator.
    val pendingSync: Boolean = false,
)
