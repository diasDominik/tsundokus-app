package uk.tsundokus.features.settings.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.preferences.AppCurrency
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
) : ViewModel() {
    private val eventChannel = Channel<SettingsEvent>()
    val events = eventChannel.receiveAsFlow()

    val state: StateFlow<SettingsState> =
        combine(
            sessionStorage.authState,
            settingsRepository.observe(),
        ) { auth, settings ->
            val user = auth?.user
            SettingsState(
                accountName = user?.username.orEmpty(),
                accountEmail = user?.email.orEmpty(),
                theme = settings.theme,
                currency = settings.currency,
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
            is SettingsAction.ChangeTheme -> viewModelScope.launch { settingsRepository.updateTheme(action.theme) }
            is SettingsAction.ChangeCurrency -> onCurrencyChange(action.currency)
            SettingsAction.SignOut -> onSignOut()
        }
    }

    private fun onCurrencyChange(currency: AppCurrency) {
        viewModelScope.launch {
            settingsRepository
                .updateCurrency(currency)
                .onSuccess {
                    eventChannel.send(
                        SettingsEvent.ShowMessage(UiText.DynamicString("Currency set to ${currency.symbol}")),
                    )
                }.onFailure { error ->
                    eventChannel.send(SettingsEvent.ShowMessage(error.toUiText()))
                }
        }
    }

    private fun onSignOut() {
        viewModelScope.launch {
            // Clear the local session; the app shell observes it and returns to the Welcome flow.
            sessionStorage.set(null)
            eventChannel.send(SettingsEvent.SignedOut)
        }
    }
}
