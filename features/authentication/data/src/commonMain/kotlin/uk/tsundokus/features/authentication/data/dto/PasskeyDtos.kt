package uk.tsundokus.features.authentication.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyCeremonySerializable(
    val ceremonyId: String,
    val optionsJson: String,
)

@Serializable
data class PasskeyFinishRequest(
    val ceremonyId: String,
    val responseJson: String,
    val label: String? = null,
)
