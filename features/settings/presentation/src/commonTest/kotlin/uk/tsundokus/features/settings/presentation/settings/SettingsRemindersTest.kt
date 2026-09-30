package uk.tsundokus.features.settings.presentation.settings

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.DeviceCurrencyProvider
import uk.tsundokus.core.domain.preferences.ReminderPreferences
import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.settings.domain.account.AccountService
import uk.tsundokus.features.settings.domain.models.AppSettings
import uk.tsundokus.features.settings.domain.settings.SettingsRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class StoredReminders : ReminderPreferences {
    val settings = MutableStateFlow(ReminderSettings())

    override fun settings(): Flow<ReminderSettings> = settings

    override suspend fun update(settings: ReminderSettings) {
        this.settings.value = settings
    }
}

private object UnusedSettings : SettingsRepository {
    override fun observe(): Flow<AppSettings> = flowOf(AppSettings())

    override suspend fun fetch(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun updateTheme(theme: ThemeMode): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun updateCurrency(currency: AppCurrency): EmptyResult<DataError.Remote> =
        Result.Success(Unit)
}

private object UnusedAccount : AccountService {
    override suspend fun getMe(): Result<User, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN)

    override suspend fun updateProfile(name: String): Result<User, DataError.Remote> = error("unused")

    override suspend fun changeEmail(
        newEmail: String,
        currentPassword: String,
    ): Result<User, DataError.Remote> = error("unused")

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun deleteAccount(): EmptyResult<DataError.Remote> = error("unused")
}

private object NoSession : SessionStorage {
    override val authState: StateFlow<AuthInfo?> = MutableStateFlow(null)

    override fun get(): AuthInfo? = null

    override fun set(info: AuthInfo?) = Unit

    override suspend fun load(): AuthInfo? = null
}

private object NoDeviceCurrency : DeviceCurrencyProvider {
    override fun currentCurrency(): AppCurrency? = null
}

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRemindersTest {
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val reminders = StoredReminders()

    private fun viewModel() =
        SettingsViewModel(
            settingsRepository = UnusedSettings,
            accountService = UnusedAccount,
            sessionStorage = NoSession,
            reminderPreferences = reminders,
            deviceCurrencyProvider = NoDeviceCurrency,
        )

    @Test
    fun `reminders start off`() =
        runTest {
            viewModel().state.test {
                assertFalse(expectMostRecentItem().reminders.enabled)
            }
        }

    @Test
    fun `switching on once permission is granted stores it`() =
        runTest {
            val sut = viewModel()
            sut.state.test {
                sut.onAction(SettingsAction.SetRemindersEnabled(true))

                assertTrue(expectMostRecentItem().reminders.enabled)
                assertTrue(reminders.settings.value.enabled)
            }
        }

    @Test
    fun `a refusal keeps them off and says notifications are blocked`() =
        runTest {
            val sut = viewModel()
            sut.state.test {
                sut.onAction(SettingsAction.NotificationsBlocked)

                val state = expectMostRecentItem()
                assertFalse(state.reminders.enabled)
                assertTrue(state.notificationsBlocked)
            }
        }

    @Test
    fun `granting permission later clears the blocked notice`() =
        runTest {
            val sut = viewModel()
            sut.state.test {
                sut.onAction(SettingsAction.NotificationsBlocked)
                sut.onAction(SettingsAction.SetRemindersEnabled(true))

                assertFalse(expectMostRecentItem().notificationsBlocked)
            }
        }

    @Test
    fun `each kind and the time are kept`() =
        runTest {
            val sut = viewModel()
            sut.onAction(SettingsAction.SetRemindersEnabled(true))
            sut.onAction(SettingsAction.SetOverdueReminders(false))
            sut.onAction(SettingsAction.SetReminderTime(hour = 18, minute = 45))

            assertEquals(
                ReminderSettings(
                    enabled = true,
                    overdue = false,
                    delayedDateReached = true,
                    hour = 18,
                    minute = 45,
                ),
                reminders.settings.value,
            )
        }

    @Test
    fun `switching off keeps the kinds and time for next time`() =
        runTest {
            val sut = viewModel()
            sut.onAction(SettingsAction.SetRemindersEnabled(true))
            sut.onAction(SettingsAction.SetDelayedDateReminders(false))
            sut.onAction(SettingsAction.SetReminderTime(hour = 7, minute = 5))
            sut.onAction(SettingsAction.SetRemindersEnabled(false))

            assertEquals(
                ReminderSettings(
                    enabled = false,
                    overdue = true,
                    delayedDateReached = false,
                    hour = 7,
                    minute = 5,
                ),
                reminders.settings.value,
            )

            sut.onAction(SettingsAction.SetRemindersEnabled(true))

            assertEquals(
                ReminderSettings(enabled = true, overdue = true, delayedDateReached = false, hour = 7, minute = 5),
                reminders.settings.value,
            )
        }
}
