package uk.tsundokus.composeapp.deeplink

import uk.tsundokus.features.authentication.presentation.navigation.EmailVerification
import uk.tsundokus.features.authentication.presentation.navigation.ResetPassword
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DeepLinkMatcherTest {
    private val matchers = buildDeepLinkMatchers(devBaseUrl = "http://localhost:8080")

    @Test
    fun verifyLink_fromProductionHttps_resolvesToEmailVerification() {
        val result = matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/verify?token=abc123")

        assertEquals(EmailVerification(token = "abc123"), result)
    }

    @Test
    fun verifyLink_fromProdApexHost_resolvesToEmailVerification() {
        val result = matchers.matchOrNull("https://tsundokus.uk/api/auth/verify?token=abc123")

        assertEquals(EmailVerification(token = "abc123"), result)
    }

    @Test
    fun verifyLink_fromCustomScheme_resolvesToEmailVerification() {
        val result = matchers.matchOrNull("tsundokus://dev.tsundokus.uk/api/auth/verify?token=abc123")

        assertEquals(EmailVerification(token = "abc123"), result)
    }

    @Test
    fun resetLink_fromProductionHttps_resolvesToResetPassword() {
        val result = matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/reset-password?token=xyz789")

        assertEquals(ResetPassword(token = "xyz789"), result)
    }

    @Test
    fun verifyLink_fromDevOrigin_resolvesToEmailVerification() {
        val result = matchers.matchOrNull("http://localhost:8080/api/auth/verify?token=dev-token")

        assertEquals(EmailVerification(token = "dev-token"), result)
    }

    @Test
    fun verifyLink_withExtraQueryParams_ignoresThem() {
        val result = matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/verify?token=abc123&utm=email")

        assertEquals(EmailVerification(token = "abc123"), result)
    }

    @Test
    fun unknownHost_doesNotMatch() {
        val result = matchers.matchOrNull("https://evil.example.com/api/auth/verify?token=abc123")

        assertNull(result)
    }

    @Test
    fun unknownPath_doesNotMatch() {
        val result = matchers.matchOrNull("https://dev.tsundokus.uk/api/auth/login?token=abc123")

        assertNull(result)
    }
}
