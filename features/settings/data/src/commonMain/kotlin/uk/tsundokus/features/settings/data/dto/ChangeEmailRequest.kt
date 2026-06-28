package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangeEmailRequest(
    val newEmail: String,
    val currentPassword: String,
)
