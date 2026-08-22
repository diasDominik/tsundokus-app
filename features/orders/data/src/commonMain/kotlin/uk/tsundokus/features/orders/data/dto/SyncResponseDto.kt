package uk.tsundokus.features.orders.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SyncResponseDto(
    val serverTime: Long,
    val changed: List<OrderDto>,
    val deleted: List<String>,
)
