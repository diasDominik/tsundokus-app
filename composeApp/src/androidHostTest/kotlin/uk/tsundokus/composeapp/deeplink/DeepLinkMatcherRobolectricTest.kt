package uk.tsundokus.composeapp.deeplink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import uk.tsundokus.features.authentication.presentation.navigation.EmailVerification
import uk.tsundokus.features.authentication.presentation.navigation.ResetPassword

/**
 * Android-host counterpart of the desktop DeepLinkMatcherTest. Runs under Robolectric so
 * [buildDeepLinkMatchers] resolves URIs through the real `android.net.Uri` — the exact path the app
 * uses on Android, which a plain JVM unit test can't exercise. `Config.NONE` (no manifest) keeps it
 * a pure unit test.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class DeepLinkMatcherRobolectricTest {
    private val matchers = buildDeepLinkMatchers(devBaseUrl = "http://localhost:8080")

    @Test
    fun verifyLink_fromProductionHttps_resolvesToEmailVerification() {
        assertEquals(
            EmailVerification(token = "abc123"),
            matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/verify?token=abc123"),
        )
    }

    @Test
    fun verifyLink_fromProdApexHost_resolvesToEmailVerification() {
        assertEquals(
            EmailVerification(token = "abc123"),
            matchers.matchOrNull("https://tsundokus.uk/api/auth/verify?token=abc123"),
        )
    }

    @Test
    fun verifyLink_fromCustomScheme_resolvesToEmailVerification() {
        assertEquals(
            EmailVerification(token = "abc123"),
            matchers.matchOrNull("tsundokus://dev.tsundokus.uk/api/auth/verify?token=abc123"),
        )
    }

    @Test
    fun resetLink_fromProductionHttps_resolvesToResetPassword() {
        assertEquals(
            ResetPassword(token = "xyz789"),
            matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/reset-password?token=xyz789"),
        )
    }

    @Test
    fun verifyLink_fromDevOrigin_resolvesToEmailVerification() {
        assertEquals(
            EmailVerification(token = "dev-token"),
            matchers.matchOrNull("http://localhost:8080/api/auth/verify?token=dev-token"),
        )
    }

    @Test
    fun verifyLink_withExtraQueryParams_ignoresThem() {
        assertEquals(
            EmailVerification(token = "abc123"),
            matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/verify?token=abc123&utm=email"),
        )
    }

    @Test
    fun unknownHost_doesNotMatch() {
        assertNull(matchers.matchOrNull("https://evil.example.com/api/auth/verify?token=abc123"))
    }

    @Test
    fun unknownPath_doesNotMatch() {
        assertNull(matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/login?token=abc123"))
    }
}
