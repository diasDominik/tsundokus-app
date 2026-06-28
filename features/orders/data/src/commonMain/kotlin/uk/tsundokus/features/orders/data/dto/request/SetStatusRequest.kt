package uk.tsundokus.features.orders.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SetStatusRequest(
    val status: String,
)
