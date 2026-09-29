package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SettingsDto(
    val theme: String,
    val currency: String,
    // False until the user (or the app, from the device's region) has picked one; the server then
    // still answers EUR, for apps that predate the flag. Absent from older servers: treated as picked.
    val currencyChosen: Boolean = true,
)
