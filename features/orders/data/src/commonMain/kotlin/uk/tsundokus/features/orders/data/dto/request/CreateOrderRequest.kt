package uk.tsundokus.features.orders.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class CreateOrderRequest(
    // Client-minted UUID: the server treats a create with an existing id as an idempotent upsert,
    // which is what lets the outbox replay this write safely.
    val id: String,
    val title: String,
    val author: String,
    val publisher: String,
    val volume: String,
    val store: String,
    val price: Double,
    val currency: String,
    val status: String,
    val readState: String,
    val orderDate: String? = null,
    val releaseDate: String? = null,
    val shipDate: String? = null,
    val eta: String? = null,
    val receivedDate: String? = null,
    val delayedTo: String? = null,
)
