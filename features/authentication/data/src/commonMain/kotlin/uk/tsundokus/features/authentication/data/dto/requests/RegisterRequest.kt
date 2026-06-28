package uk.tsundokus.features.authentication.data.dto.requests

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
)
