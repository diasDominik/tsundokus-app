package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    val theme: String,
    val currency: String,
)
