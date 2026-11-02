package uk.tsundokus.core.data

/** Public re-export of build-time app metadata. [BuildKonfig] itself is internal to this module. */
object AppBuildInfo {
    /** The installed app version: "1.2.0" for a build of tag v1.2.0, "1.2.0-dev" for any later commit. */
    val version: String = BuildKonfig.APP_VERSION

    /** Backend HTTP base URL (e.g. "https://api.tsundokus.uk"). */
    val baseUrl: String = BuildKonfig.BASE_URL_HTTP

    /** Static API key sent with every request; also needed on the WebSocket handshake. */
    val apiKey: String = BuildKonfig.API_KEY
}
