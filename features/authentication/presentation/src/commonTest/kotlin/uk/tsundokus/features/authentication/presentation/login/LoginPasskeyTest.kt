package uk.tsundokus.features.authentication.presentation.login

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.error_passkey_rejected
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.PasskeyCeremony
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.auth.UserType
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.authentication.testing.FakeAuthService
import uk.tsundokus.features.authentication.testing.FakeStaleSessionStore
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class LoginPasskeyTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun theServersChallengeIsHandedToTheAuthenticatorUnchanged() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    beginPasskeyLoginResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                )
            val viewModel = createViewModel(authService)

            viewModel.events.test {
                viewModel.onPasskeyLogin()

                val event = assertIs<LoginEvent.RunPasskeyCeremony>(awaitItem())
                assertEquals(OPTIONS, event.optionsJson)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun theSignedResponseGoesBackWithTheCeremonyItAnswers() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    beginPasskeyLoginResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                    finishPasskeyLoginResult = Result.Success(FAKE_AUTH_INFO),
                )
            val viewModel = createViewModel(authService)

            viewModel.events.test {
                viewModel.onPasskeyLogin()
                awaitItem() // the ceremony request
                viewModel.onPasskeyResponse(RESPONSE)

                assertEquals(LoginEvent.LoginSuccess, awaitItem())
                assertEquals(listOf("ceremony-1" to RESPONSE), authService.finishPasskeyLoginCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun aResponseTheServerRefusesIsReportedAsSuch() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    beginPasskeyLoginResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                    finishPasskeyLoginResult = Result.Failure(DataError.Remote.UNAUTHORIZED),
                )
            val viewModel = createViewModel(authService)

            viewModel.events.test {
                viewModel.onPasskeyLogin()
                awaitItem()
                viewModel.onPasskeyResponse(RESPONSE)

                val event = assertIs<LoginEvent.LoginFailure>(awaitItem())
                assertEquals(
                    UiText.Resource(Res.string.error_passkey_rejected).id,
                    (event.error as UiText.Resource).id,
                )
                assertFalse(viewModel.state.value.isSigningInWithPasskey)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun aResponseThatAnswersNoCeremonyIsIgnored() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(finishPasskeyLoginResult = Result.Success(FAKE_AUTH_INFO))
            val viewModel = createViewModel(authService)

            viewModel.onPasskeyResponse(RESPONSE)

            assertTrue(authService.finishPasskeyLoginCalls.isEmpty())
        }

    @Test
    fun cancellingAtTheAuthenticatorSaysNothingAndLeavesTheFormUsable() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    beginPasskeyLoginResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                )
            val viewModel = createViewModel(authService)

            viewModel.events.test {
                viewModel.onPasskeyLogin()
                awaitItem()
                viewModel.onPasskeyCeremonyFailed(message = null)

                expectNoEvents()
                assertFalse(viewModel.state.value.isSigningInWithPasskey)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun aSecondPressWhileTheAuthenticatorIsOpenStartsNothingNew() =
        runTest(testDispatcher) {
            val authService =
                FakeAuthService(
                    beginPasskeyLoginResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                )
            val viewModel = createViewModel(authService)

            viewModel.events.test {
                viewModel.onPasskeyLogin()
                awaitItem()
                viewModel.onPasskeyLogin()

                expectNoEvents()
                assertEquals(1, authService.beginPasskeyLoginCalls)
                cancelAndConsumeRemainingEvents()
            }
        }

    private fun createViewModel(authService: FakeAuthService): LoginViewModel =
        LoginViewModel(
            authService = authService,
            staleSessionStore = FakeStaleSessionStore(),
        )

    private companion object {
        private const val OPTIONS = """{"publicKey":{"challenge":"abc","rpId":"tsundokus.uk"}}"""
        private const val RESPONSE = """{"id":"cred","response":{"signature":"sig"}}"""

        private val FAKE_AUTH_INFO =
            AuthInfo(
                accessToken = "token",
                refreshToken = "refresh",
                user =
                    User(
                        id = "1",
                        email = "test@test.com",
                        username = "test",
                        hasVerifiedEmail = true,
                        userType = UserType.REGISTERED,
                    ),
            )
    }
}
