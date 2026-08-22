package uk.tsundokus.core.data

/** Public re-export of build-time app metadata. [BuildKonfig] itself is internal to this module. */
object AppBuildInfo {
    /** The installed app version (e.g. "1.2.0"), used by the app-update check. */
    val version: String = BuildKonfig.APP_VERSION

    /** Backend HTTP base URL (e.g. "https://api.tsundokus.uk"). */
    val baseUrl: String = BuildKonfig.BASE_URL_HTTP

    /** Static API key sent with every request; also needed on the WebSocket handshake. */
    val apiKey: String = BuildKonfig.API_KEY
}
