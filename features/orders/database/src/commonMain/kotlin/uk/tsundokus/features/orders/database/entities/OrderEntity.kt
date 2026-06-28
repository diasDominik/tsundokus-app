package uk.tsundokus.features.orders.database.entities

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
    val createdAt: Long,
)
