package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class PasskeyCeremonySerializable(
    val ceremonyId: String,
    val optionsJson: String,
)

@Serializable
data class PasskeySerializable(
    val credentialId: String,
    val label: String? = null,
    val createdAt: String,
    val lastUsedAt: String? = null,
)

@Serializable
data class PasskeyFinishRequest(
    val ceremonyId: String,
    val responseJson: String,
    val label: String? = null,
)
