package uk.tsundokus.core.domain.auth

/**
 * A passkey ceremony the server has started.
 *
 * [optionsJson] is WebAuthn's own request envelope, handed to the platform authenticator unread —
 * the challenge inside it is the server's, and altering any of it would only make the response fail
 * verification. [ceremonyId] identifies the challenge when the signed response goes back.
 */
data class PasskeyCeremony(
    val ceremonyId: String,
    val optionsJson: String,
)
