package uk.tsundokus.features.settings.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String,
)
