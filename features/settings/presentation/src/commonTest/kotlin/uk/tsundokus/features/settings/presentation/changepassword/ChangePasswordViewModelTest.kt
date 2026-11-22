package uk.tsundokus.features.settings.presentation.changepassword

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.change_password_error_requirements
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.settings.domain.account.AccountService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeAccountService : AccountService {
    val changePasswordCalls: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun getMe(): Result<User, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun updateProfile(name: String): Result<User, DataError.Remote> =
        Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun changeEmail(
        newEmail: String,
        currentPassword: String,
    ): Result<User, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> {
        changePasswordCalls += currentPassword to newPassword
        return Result.Success(Unit)
    }

    override suspend fun deleteAccount(): EmptyResult<DataError.Remote> = Result.Success(Unit)
}

@OptIn(ExperimentalCoroutinesApi::class)
class ChangePasswordViewModelTest {
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
    fun aPasswordTheSignUpFormWouldRejectIsRejectedHereToo() =
        runTest(testDispatcher) {
            listOf("abc1234", "abcdefgh", "a1".repeat(21)).forEach { candidate ->
                val service = FakeAccountService()
                val viewModel = ChangePasswordViewModel(service)
                viewModel.fill(current = "old-pass1", new = candidate)

                viewModel.events.test {
                    viewModel.onSave()

                    val error = assertIs<ChangePasswordEvent.Error>(awaitItem())
                    val message = assertIs<UiText.Resource>(error.message)
                    assertEquals(Res.string.change_password_error_requirements, message.id, candidate)
                    cancelAndConsumeRemainingEvents()
                }
                assertTrue(service.changePasswordCalls.isEmpty(), candidate)
            }
        }

    @Test
    fun aPasswordMeetingTheSignUpRulesIsSent() =
        runTest(testDispatcher) {
            val service = FakeAccountService()
            val viewModel = ChangePasswordViewModel(service)
            viewModel.fill(current = "old-pass1", new = "new-pass1")

            viewModel.events.test {
                viewModel.onSave()

                assertIs<ChangePasswordEvent.Saved>(awaitItem())
                cancelAndConsumeRemainingEvents()
            }
            assertEquals(listOf("old-pass1" to "new-pass1"), service.changePasswordCalls)
        }

    private fun ChangePasswordViewModel.fill(
        current: String,
        new: String,
    ) {
        currentPasswordState.setText(current)
        newPasswordState.setText(new)
        confirmPasswordState.setText(new)
    }

    private fun TextFieldState.setText(text: String) = setTextAndPlaceCursorAtEnd(text)
}
