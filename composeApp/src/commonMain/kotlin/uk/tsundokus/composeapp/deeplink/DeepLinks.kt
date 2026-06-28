package uk.tsundokus.composeapp.deeplink

import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.deeplink.DeepLinkMatcher
import androidx.navigation3.runtime.deeplink.DeepLinkRequest
import androidx.navigation3.runtime.deeplink.DeepLinkUri
import androidx.navigation3.runtime.deeplink.UriDeepLinkMatcher
import uk.tsundokus.features.authentication.presentation.navigation.EmailVerification
import uk.tsundokus.features.authentication.presentation.navigation.ResetPassword
import kotlinx.serialization.serializer

// Server-emitted link paths (see notification EmailService): {origin}{path}?token=<token>.
private const val VERIFY_PATH = "/api/auth/verify"
private const val RESET_PATH = "/api/auth/reset-password"

/**
 * Builds the official Navigation3 [UriDeepLinkMatcher]s for the verify / reset-password links.
 *
 * [UriDeepLinkMatcher] matches scheme + authority exactly, so a matcher is registered for every
 * origin the link can arrive from: the production App Link host (`https`) and custom scheme
 * (`tsundoku`) declared in the Android manifest, plus the dev origin from [devBaseUrl]
 * (BuildKonfig.BASE_URL_HTTP) used by local/device email testing. `?token={token}` extracts the
 * token, which the matcher's serializer deserializes straight into the [NavKey].
 */
fun buildDeepLinkMatchers(devBaseUrl: String): List<DeepLinkMatcher<out NavKey>> {
    val dev = DeepLinkUri(devBaseUrl)
    val origins =
        buildSet {
            // Prod (apex) + dev hosts, App Link (https) and custom scheme each.
            add("https" to "tsundokus.uk")
            add("tsundokus" to "tsundokus.uk")
            add("https" to "dev.tsundokus.uk")
            add("tsundokus" to "dev.tsundokus.uk")
            // Plus the dev origin from BuildKonfig.BASE_URL_HTTP (local/device email testing).
            val devScheme = dev.getScheme()
            val devAuthority = dev.getAuthority()
            if (devScheme != null && devAuthority != null) add(devScheme to devAuthority)
        }

    return origins.flatMap { (scheme, authority) ->
        listOf(
            UriDeepLinkMatcher(
                uriPattern = DeepLinkUri("$scheme://$authority$VERIFY_PATH?token={token}"),
                serializer = serializer<EmailVerification>(),
            ),
            UriDeepLinkMatcher(
                uriPattern = DeepLinkUri("$scheme://$authority$RESET_PATH?token={token}"),
                serializer = serializer<ResetPassword>(),
            ),
        )
    }
}

/** Resolves a raw deep-link URI to its [NavKey] using the registered matchers, or null if none match. */
fun List<DeepLinkMatcher<out NavKey>>.matchOrNull(uri: String): NavKey? {
    val request = DeepLinkRequest.fromUriString(uri)
    return firstNotNullOfOrNull { it.match(request)?.key }
}
