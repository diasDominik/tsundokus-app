package uk.tsundokus.features.orders.data.dto

import kotlinx.serialization.Serializable

/** A realtime order change pushed over the WebSocket. Mirrors the server's OrderChangeMessage. */
@Serializable
data class OrderChangeDto(
    val type: String,
    val order: OrderDto? = null,
    val id: String? = null,
    val serverTime: Long,
)
