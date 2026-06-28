package uk.tsundokus.features.settings.presentation.settings

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode

sealed interface SettingsAction {
    data class ChangeTheme(val theme: ThemeMode) : SettingsAction

    data class ChangeCurrency(val currency: AppCurrency) : SettingsAction

    data object SignOut : SettingsAction
}
