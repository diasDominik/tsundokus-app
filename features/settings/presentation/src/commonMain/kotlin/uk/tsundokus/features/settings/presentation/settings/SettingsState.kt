package uk.tsundokus.features.settings.presentation.settings

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode

data class SettingsState(
    val accountName: String = "",
    val accountEmail: String = "",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val currency: AppCurrency = AppCurrency.EUR,
)
