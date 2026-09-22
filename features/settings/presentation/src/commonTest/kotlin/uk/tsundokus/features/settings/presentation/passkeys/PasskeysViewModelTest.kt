package uk.tsundokus.features.settings.presentation.passkeys

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.auth.PasskeyCeremony
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.settings.domain.passkeys.Passkey
import uk.tsundokus.features.settings.domain.passkeys.PasskeyService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakePasskeyService(
    var passkeysResult: Result<List<Passkey>, DataError.Remote> = Result.Success(emptyList()),
    var beginEnrolmentResult: Result<PasskeyCeremony, DataError.Remote> =
        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = "{}")),
    var finishEnrolmentResult: Result<Passkey, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN),
    var deleteResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : PasskeyService {
    val finishEnrolmentCalls: MutableList<Pair<String, String>> = mutableListOf()
    val deleteCalls: MutableList<String> = mutableListOf()
    var beginEnrolmentCalls: Int = 0
        private set

    override suspend fun passkeys(): Result<List<Passkey>, DataError.Remote> = passkeysResult

    override suspend fun beginEnrolment(): Result<PasskeyCeremony, DataError.Remote> {
        beginEnrolmentCalls++
        return beginEnrolmentResult
    }

    override suspend fun finishEnrolment(
        ceremonyId: String,
        responseJson: String,
        label: String?,
    ): Result<Passkey, DataError.Remote> {
        finishEnrolmentCalls += ceremonyId to responseJson
        return finishEnrolmentResult
    }

    override suspend fun delete(credentialId: String): EmptyResult<DataError.Remote> {
        deleteCalls += credentialId
        return deleteResult
    }
}

private fun passkey(id: String) =
    Passkey(credentialId = id, label = null, createdAt = "2026-09-22T10:00:00Z", lastUsedAt = null)

@OptIn(ExperimentalCoroutinesApi::class)
class PasskeysViewModelTest {
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
    fun theAccountsPasskeysAreListedOnOpen() =
        runTest(testDispatcher) {
            val service = FakePasskeyService(passkeysResult = Result.Success(listOf(passkey("a"), passkey("b"))))

            val viewModel = PasskeysViewModel(service)

            assertEquals(
                listOf("a", "b"),
                viewModel.state.value.passkeys
                    .map { it.credentialId },
            )
            assertFalse(viewModel.state.value.isLoading)
        }

    @Test
    fun theServersChallengeIsHandedToTheAuthenticatorUnchanged() =
        runTest(testDispatcher) {
            val service =
                FakePasskeyService(
                    beginEnrolmentResult =
                        Result.Success(PasskeyCeremony(ceremonyId = "ceremony-1", optionsJson = OPTIONS)),
                )
            val viewModel = PasskeysViewModel(service)

            viewModel.events.test {
                viewModel.onAddPasskey()

                val event = assertIs<PasskeysEvent.RunEnrolmentCeremony>(awaitItem())
                assertEquals(OPTIONS, event.optionsJson)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun anEnrolledPasskeyJoinsTheListWithoutAnotherRoundTrip() =
        runTest(testDispatcher) {
            val service = FakePasskeyService(finishEnrolmentResult = Result.Success(passkey("new")))
            val viewModel = PasskeysViewModel(service)

            viewModel.events.test {
                viewModel.onAddPasskey()
                awaitItem() // the ceremony request
                viewModel.onEnrolmentResponse(RESPONSE)

                assertIs<PasskeysEvent.ShowMessage>(awaitItem())
                assertEquals(listOf("ceremony-1" to RESPONSE), service.finishEnrolmentCalls)
                assertEquals(
                    listOf("new"),
                    viewModel.state.value.passkeys
                        .map { it.credentialId },
                )
                assertFalse(viewModel.state.value.isEnrolling)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun aResponseThatAnswersNoCeremonyIsIgnored() =
        runTest(testDispatcher) {
            val service = FakePasskeyService(finishEnrolmentResult = Result.Success(passkey("new")))
            val viewModel = PasskeysViewModel(service)

            viewModel.onEnrolmentResponse(RESPONSE)

            assertTrue(service.finishEnrolmentCalls.isEmpty())
        }

    @Test
    fun cancellingAtTheAuthenticatorSaysNothing() =
        runTest(testDispatcher) {
            val viewModel = PasskeysViewModel(FakePasskeyService())

            viewModel.events.test {
                viewModel.onAddPasskey()
                awaitItem()
                viewModel.onEnrolmentCeremonyFailed(message = null)

                expectNoEvents()
                assertFalse(viewModel.state.value.isEnrolling)
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun removalIsConfirmedBeforeAnythingLeaves() =
        runTest(testDispatcher) {
            val service = FakePasskeyService(passkeysResult = Result.Success(listOf(passkey("a"))))
            val viewModel = PasskeysViewModel(service)

            viewModel.onRemoveRequested(passkey("a"))
            assertEquals(
                "a",
                viewModel.state.value.removing
                    ?.credentialId,
            )

            viewModel.onRemoveDismissed()
            assertEquals(null, viewModel.state.value.removing)
            assertTrue(service.deleteCalls.isEmpty())
            assertEquals(
                listOf("a"),
                viewModel.state.value.passkeys
                    .map { it.credentialId },
            )
        }

    @Test
    fun aRemovedPasskeyLeavesTheList() =
        runTest(testDispatcher) {
            val service = FakePasskeyService(passkeysResult = Result.Success(listOf(passkey("a"), passkey("b"))))
            val viewModel = PasskeysViewModel(service)

            viewModel.events.test {
                viewModel.onRemoveRequested(passkey("a"))
                viewModel.onRemoveConfirmed()

                assertIs<PasskeysEvent.ShowMessage>(awaitItem())
                assertEquals(listOf("a"), service.deleteCalls)
                assertEquals(
                    listOf("b"),
                    viewModel.state.value.passkeys
                        .map { it.credentialId },
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    @Test
    fun aRemovalTheServerRefusesLeavesTheListAlone() =
        runTest(testDispatcher) {
            val service =
                FakePasskeyService(
                    passkeysResult = Result.Success(listOf(passkey("a"))),
                    deleteResult = Result.Failure(DataError.Remote.SERVER_ERROR),
                )
            val viewModel = PasskeysViewModel(service)

            viewModel.events.test {
                viewModel.onRemoveRequested(passkey("a"))
                viewModel.onRemoveConfirmed()

                assertIs<PasskeysEvent.ShowMessage>(awaitItem())
                assertEquals(
                    listOf("a"),
                    viewModel.state.value.passkeys
                        .map { it.credentialId },
                )
                cancelAndConsumeRemainingEvents()
            }
        }

    private companion object {
        private const val OPTIONS = """{"publicKey":{"challenge":"abc","rp":{"id":"tsundokus.uk"}}}"""
        private const val RESPONSE = """{"id":"cred","response":{"attestationObject":"att"}}"""
    }
}
