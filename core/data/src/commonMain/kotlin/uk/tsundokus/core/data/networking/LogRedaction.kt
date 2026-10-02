package uk.tsundokus.core.data.networking

/** Carries the API key on every request; masked in logs like the Authorization header. */
internal const val API_KEY_HEADER = "x-api-key"

private const val MASK = "***"

/**
 * JSON fields that hold a secret: what the auth, password and reset requests send and the session
 * responses return. The value is matched as a JSON string, escaped quotes included.
 */
private val SECRET_JSON_FIELD =
    Regex(
        "\"(password|currentPassword|newPassword|accessToken|refreshToken|token)\"\\s*:\\s*\"(?:[^\"\\\\]|\\\\.)*\"",
    )

/** The order WebSocket authenticates in its query string, since browsers can't set WebSocket headers. */
private val SECRET_QUERY_PARAMETER = Regex("([?&](?:token|apiKey)=)[^&#\\s]*")

/**
 * [message] with every secret masked, so a log line can name the request and its shape but never
 * hand over a session, a password or the API key. Applied to everything the HTTP client logs, which
 * includes release builds.
 */
internal fun redactSecrets(message: String): String =
    message
        .replace(SECRET_JSON_FIELD) { match -> "\"${match.groupValues[1]}\":\"$MASK\"" }
        .replace(SECRET_QUERY_PARAMETER) { match -> match.groupValues[1] + MASK }
