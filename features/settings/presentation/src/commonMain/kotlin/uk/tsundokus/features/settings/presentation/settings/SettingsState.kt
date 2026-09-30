package uk.tsundokus.features.settings.presentation.settings

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.presentation.notifications.areRemindersSupported

data class SettingsState(
    val accountName: String = "",
    val accountEmail: String = "",
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val currency: AppCurrency = AppCurrency.EUR,
    val suggestedCurrencies: List<AppCurrency> = emptyList(),
    val reminders: ReminderSettings = ReminderSettings(),
    /** Desktop and the web can't notify while closed, so they don't show the reminders card. */
    val remindersSupported: Boolean = areRemindersSupported(),
    /** The user refused notification permission; only the system settings can change that now. */
    val notificationsBlocked: Boolean = false,
)
