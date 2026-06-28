package uk.tsundokus.core.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class UserTypeSerializable {
    REGISTERED,
    ANONYMOUS,
}

@Serializable
data class UserSerializable(
    val id: String,
    val email: String,
    // Server emits the account's display name under "name"; mapped to the app's [username] field.
    @SerialName("name") val username: String,
    val hasVerifiedEmail: Boolean = true,
    val userType: UserTypeSerializable = UserTypeSerializable.REGISTERED,
)
