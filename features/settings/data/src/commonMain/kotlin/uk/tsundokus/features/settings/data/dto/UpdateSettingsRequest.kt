package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateSettingsRequest(
    val theme: String? = null,
    val currency: String? = null,
)
