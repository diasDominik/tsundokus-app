package uk.tsundokus.features.settings.domain.passkeys

/**
 * A passkey registered to the account.
 *
 * [credentialId] is the authenticator's own id for it — public, and what a removal names. The dates
 * are the server's ISO-8601 instants; the UI shows the day, not the instant.
 */
data class Passkey(
    val credentialId: String,
    val label: String?,
    val createdAt: String,
    val lastUsedAt: String?,
)
