package uk.tsundokus.features.orders.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class OrderDto(
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
    val orderDate: String?,
    val releaseDate: String?,
    val shipDate: String?,
    val eta: String?,
    val receivedDate: String?,
    val delayedTo: String?,
    // Defaulted so a response from a server that predates ISBNs still decodes.
    val isbn: String? = null,
    val hasCover: Boolean = false,
    val createdAt: Long,
)
