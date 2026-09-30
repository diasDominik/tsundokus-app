package uk.tsundokus.features.settings.presentation.settings

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode

sealed interface SettingsAction {
    data class ChangeTheme(val theme: ThemeMode) : SettingsAction

    data class ChangeCurrency(val currency: AppCurrency) : SettingsAction

    /** Switching on only arrives once notification permission is granted. */
    data class SetRemindersEnabled(val enabled: Boolean) : SettingsAction

    /** Permission was refused, so reminders stay off. */
    data object NotificationsBlocked : SettingsAction

    data class SetOverdueReminders(val enabled: Boolean) : SettingsAction

    data class SetDelayedDateReminders(val enabled: Boolean) : SettingsAction

    data class SetReminderTime(
        val hour: Int,
        val minute: Int,
    ) : SettingsAction

    data object SignOut : SettingsAction
}
