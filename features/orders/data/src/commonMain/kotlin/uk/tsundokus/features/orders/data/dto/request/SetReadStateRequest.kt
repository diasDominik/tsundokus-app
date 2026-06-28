package uk.tsundokus.features.orders.data.dto.request

import kotlinx.serialization.Serializable

@Serializable
data class SetReadStateRequest(
    val readState: String,
)
