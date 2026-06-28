package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateProfileRequest(
    val name: String,
)
