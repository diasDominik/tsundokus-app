package uk.tsundokus.features.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import tsundokuapp.features.settings.presentation.generated.resources.Res
import tsundokuapp.features.settings.presentation.generated.resources.settings_currency_changed
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.DeviceCurrencyProvider
import uk.tsundokus.core.domain.preferences.ReminderPreferences
import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.core.presentation.util.UiText
import uk.tsundokus.core.presentation.util.toUiText
import uk.tsundokus.features.settings.domain.account.AccountService
import uk.tsundokus.features.settings.domain.settings.SettingsRepository
import kotlin.time.Duration.Companion.seconds

@KoinViewModel
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val accountService: AccountService,
    private val sessionStorage: SessionStorage,
    private val reminderPreferences: ReminderPreferences,
    deviceCurrencyProvider: DeviceCurrencyProvider,
) : ViewModel() {
    /** The device's own currency and the long-standing three, offered first in the picker. */
    private val suggestedCurrencies =
        listOfNotNull(deviceCurrencyProvider.currentCurrency(), AppCurrency.EUR, AppCurrency.USD, AppCurrency.GBP)
            .distinctBy(AppCurrency::code)

    private val eventChannel = Channel<SettingsEvent>()
    val events = eventChannel.receiveAsFlow()

    private val notificationsBlocked = MutableStateFlow(false)

    val state: StateFlow<SettingsState> =
        combine(
            sessionStorage.authState,
            settingsRepository.observe(),
            reminderPreferences.settings(),
            notificationsBlocked,
        ) { auth, settings, reminders, blocked ->
            val user = auth?.user
            SettingsState(
                accountName = user?.username.orEmpty(),
                accountEmail = user?.email.orEmpty(),
                theme = settings.theme,
                currency = settings.currency,
                suggestedCurrencies = suggestedCurrencies,
                reminders = reminders,
                notificationsBlocked = blocked,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5.seconds),
            initialValue = SettingsState(),
        )

    init {
        // Pull the latest server-owned settings; failures fall back to cached/default values.
        viewModelScope.launch { settingsRepository.fetch() }
        // Refresh the signed-in account so the header name/email stay in sync with the server.
        viewModelScope.launch { accountService.getMe() }
    }

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ChangeTheme -> {
                viewModelScope.launch { settingsRepository.updateTheme(action.theme) }
            }

            is SettingsAction.ChangeCurrency -> {
                onCurrencyChange(action.currency)
            }

            is SettingsAction.SetRemindersEnabled -> {
                updateReminders { it.copy(enabled = action.enabled) }
            }

            SettingsAction.NotificationsBlocked -> {
                notificationsBlocked.value = true
            }

            is SettingsAction.SetOverdueReminders -> {
                updateReminders { it.copy(overdue = action.enabled) }
            }

            is SettingsAction.SetDelayedDateReminders -> {
                updateReminders {
                    it.copy(
                        delayedDateReached = action.enabled,
                    )
                }
            }

            is SettingsAction.SetReminderTime -> {
                updateReminders {
                    it.copy(
                        hour = action.hour,
                        minute = action.minute,
                    )
                }
            }

            SettingsAction.SignOut -> {
                onSignOut()
            }
        }
    }

    private fun onCurrencyChange(currency: AppCurrency) {
        viewModelScope.launch {
            settingsRepository
                .updateCurrency(currency)
                .onSuccess {
                    eventChannel.send(
                        SettingsEvent.ShowMessage(
                            UiText.Resource(Res.string.settings_currency_changed, arrayOf(currency.code)),
                        ),
                    )
                }.onFailure { error ->
                    eventChannel.send(SettingsEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    private fun updateReminders(change: (ReminderSettings) -> ReminderSettings) {
        viewModelScope.launch {
            val updated = change(reminderPreferences.settings().first())
            // Reminders on means permission was just granted, so any earlier refusal is over.
            if (updated.enabled) notificationsBlocked.value = false
            reminderPreferences.update(updated)
        }
    }

    private fun onSignOut() {
        viewModelScope.launch {
            // Drop the session; the shell observes it, wipes the local cache (see MainViewModel) and
            // returns to the Welcome flow.
            sessionStorage.set(null)
            eventChannel.send(SettingsEvent.SignedOut)
        }
    }
}
