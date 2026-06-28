package uk.tsundokus.features.authentication.presentation.register

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.authentication.presentation.generated.resources.Res
import tsundokuapp.features.authentication.presentation.generated.resources.error_account_exists
import tsundokuapp.features.authentication.presentation.generated.resources.error_display_name_invalid
import tsundokuapp.features.authentication.presentation.generated.resources.register_email_invalid
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_mismatch
import tsundokuapp.features.authentication.presentation.generated.resources.register_password_requirements
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.auth.UserType
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.features.authentication.testing.FakeAuthService
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {
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
    fun `register with invalid inputs sets field errors and does not call auth service`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = RegisterViewModel(authService)
            activateState(viewModel)

            viewModel.register()

            assertUiTextResource(viewModel.state.value.displayNameError, Res.string.error_display_name_invalid)
            assertUiTextResource(viewModel.state.value.emailError, Res.string.register_email_invalid)
            assertUiTextResource(viewModel.state.value.passwordError, Res.string.register_password_requirements)
            assertNull(viewModel.state.value.confirmPasswordError)
            assertEquals(0, authService.registerCalls)
        }

    @Test
    fun `validateDisplayNameOnBlur with blank display name sets display name error`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.displayNameTextState
                .setTextAndPlaceCursorAtEnd("   ")
            viewModel.validateDisplayNameOnBlur()

            assertUiTextResource(viewModel.state.value.displayNameError, Res.string.error_display_name_invalid)
        }

    @Test
    fun `validateEmailOnBlur with invalid email sets email error`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.emailTextState
                .setTextAndPlaceCursorAtEnd("invalid-email")
            viewModel.validateEmailOnBlur()

            assertUiTextResource(viewModel.state.value.emailError, Res.string.register_email_invalid)
        }

    @Test
    fun `validatePasswordOnBlur with weak password sets password error`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.passwordTextState
                .setTextAndPlaceCursorAtEnd("weakpass")
            viewModel.validatePasswordOnBlur()

            assertUiTextResource(viewModel.state.value.passwordError, Res.string.register_password_requirements)
        }

    @Test
    fun `validateConfirmPasswordOnBlur with mismatched passwords sets confirm password error`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.passwordTextState
                .setTextAndPlaceCursorAtEnd("ValidPass1")
            viewModel.state.value.confirmPasswordTextState
                .setTextAndPlaceCursorAtEnd("ValidPass2")
            viewModel.validateConfirmPasswordOnBlur()

            assertUiTextResource(viewModel.state.value.confirmPasswordError, Res.string.register_password_mismatch)
        }

    @Test
    fun `validateDisplayNameOnBlur clears display name error after user fixes invalid input`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.displayNameTextState
                .setTextAndPlaceCursorAtEnd("   ")
            viewModel.validateDisplayNameOnBlur()
            assertNotNull(viewModel.state.value.displayNameError)

            viewModel.state.value.displayNameTextState
                .setTextAndPlaceCursorAtEnd("Valid Name")
            viewModel.validateDisplayNameOnBlur()

            assertNull(viewModel.state.value.displayNameError)
        }

    @Test
    fun `validateConfirmPasswordOnBlur clears confirm password error after user fixes invalid input`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            viewModel.state.value.passwordTextState
                .setTextAndPlaceCursorAtEnd("ValidPass1")
            viewModel.state.value.confirmPasswordTextState
                .setTextAndPlaceCursorAtEnd("ValidPass2")
            viewModel.validateConfirmPasswordOnBlur()
            assertNotNull(viewModel.state.value.confirmPasswordError)

            viewModel.state.value.confirmPasswordTextState
                .setTextAndPlaceCursorAtEnd("ValidPass1")
            viewModel.validateConfirmPasswordOnBlur()

            assertNull(viewModel.state.value.confirmPasswordError)
        }

    @Test
    fun `togglePasswordVisibility toggles visibility state`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            assertEquals(false, viewModel.state.value.isPasswordVisible)

            viewModel.togglePasswordVisibility()
            assertEquals(true, viewModel.state.value.isPasswordVisible)

            viewModel.togglePasswordVisibility()
            assertEquals(false, viewModel.state.value.isPasswordVisible)
        }

    @Test
    fun `toggleConfirmPasswordVisibility toggles visibility state independently`() =
        runTest(testDispatcher) {
            val viewModel = createViewModel()

            assertEquals(false, viewModel.state.value.isPasswordVisible)
            assertEquals(false, viewModel.state.value.isConfirmPasswordVisible)

            viewModel.toggleConfirmPasswordVisibility()
            assertEquals(false, viewModel.state.value.isPasswordVisible)
            assertEquals(true, viewModel.state.value.isConfirmPasswordVisible)

            viewModel.togglePasswordVisibility()
            assertEquals(true, viewModel.state.value.isPasswordVisible)
            assertEquals(true, viewModel.state.value.isConfirmPasswordVisible)
        }

    @Test
    fun `register with mismatched confirm password sets confirm error and does not call auth service`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService()
            val viewModel = RegisterViewModel(authService)
            activateState(viewModel)
            fillValidInputs(viewModel)
            viewModel.state.value.confirmPasswordTextState
                .setTextAndPlaceCursorAtEnd("DifferentPass1")

            viewModel.register()

            assertUiTextResource(viewModel.state.value.confirmPasswordError, Res.string.register_password_mismatch)
            assertEquals(0, authService.registerCalls)
        }

    @Test
    fun `register with valid inputs emits success event`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(registerResult = Result.Success(FAKE_AUTH_INFO))
            val viewModel = RegisterViewModel(authService)
            activateState(viewModel)
            fillValidInputs(viewModel)

            viewModel.events.test {
                viewModel.register()

                assertEquals(RegisterEvent.RegistrationSuccess, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, authService.registerCalls)
            assertNull(viewModel.state.value.displayNameError)
            assertNull(viewModel.state.value.emailError)
            assertNull(viewModel.state.value.passwordError)
            assertNull(viewModel.state.value.confirmPasswordError)
        }

    @Test
    fun `register conflict emits registration error event with account exists message`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(registerResult = Result.Failure(DataError.Remote.CONFLICT))
            val viewModel = RegisterViewModel(authService)
            activateState(viewModel)
            fillValidInputs(viewModel)

            viewModel.events.test {
                viewModel.register()

                val event = awaitItem()
                val errorEvent = assertIs<RegisterEvent.RegistrationError>(event)
                assertUiTextResource(errorEvent.message, Res.string.error_account_exists)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, authService.registerCalls)
            assertEquals(false, viewModel.state.value.isRegistering)
        }

    @Test
    fun `register server error emits registration error event with generic server message`() =
        runTest(testDispatcher) {
            val authService = FakeAuthService(registerResult = Result.Failure(DataError.Remote.SERVER_ERROR))
            val viewModel = RegisterViewModel(authService)
            activateState(viewModel)
            fillValidInputs(viewModel)

            viewModel.events.test {
                viewModel.register()

                val event = awaitItem()
                val errorEvent = assertIs<RegisterEvent.RegistrationError>(event)
                val message = assertIs<UiText.Resource>(errorEvent.message)
                assertEquals(false, message.id == Res.string.error_account_exists)
                cancelAndIgnoreRemainingEvents()
            }

            assertEquals(1, authService.registerCalls)
            assertEquals(false, viewModel.state.value.isRegistering)
        }

    private fun TestScope.activateState(viewModel: RegisterViewModel) {
        backgroundScope.launch {
            viewModel.state.collect()
        }
        advanceUntilIdle()
    }

    private fun fillValidInputs(viewModel: RegisterViewModel) {
        viewModel.state.value.displayNameTextState
            .setTextAndPlaceCursorAtEnd("Valid Name")
        viewModel.state.value.emailTextState
            .setTextAndPlaceCursorAtEnd("test@example.com")
        viewModel.state.value.passwordTextState
            .setTextAndPlaceCursorAtEnd("ValidPass1")
        viewModel.state.value.confirmPasswordTextState
            .setTextAndPlaceCursorAtEnd("ValidPass1")
    }

    private fun assertUiTextResource(
        uiText: UiText?,
        expected: StringResource,
    ) {
        val resource = assertIs<UiText.Resource>(uiText)
        assertEquals(expected, resource.id)
    }

    private fun TestScope.createViewModel(): RegisterViewModel {
        val viewModel = RegisterViewModel(FakeAuthService())
        activateState(viewModel)
        return viewModel
    }

    private companion object {
        private val FAKE_AUTH_INFO =
            AuthInfo(
                accessToken = "token",
                refreshToken = "refresh",
                user =
                    User(
                        id = "1",
                        email = "test@example.com",
                        username = "Valid Name",
                        hasVerifiedEmail = true,
                        userType = UserType.REGISTERED,
                    ),
            )
    }
}
