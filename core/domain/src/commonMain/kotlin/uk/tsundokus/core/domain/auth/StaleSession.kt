package uk.tsundokus.core.domain.auth

import kotlinx.serialization.Serializable

/**
 * The account whose session died involuntarily (token rejected) while the local order cache was
 * still on the device.
 *
 * Recorded just before [SessionStorage] is cleared so the app knows *who* to ask back in: the local
 * database is only safe to keep while that same account signs in again. If a *different* account
 * signs in, the cache belongs to someone else and must be wiped ([userId] is how the sign-in flow
 * tells the two apart).
 */
@Serializable
data class StaleSession(
    val userId: String,
    val email: String,
    val username: String,
    val userType: UserType,
)
