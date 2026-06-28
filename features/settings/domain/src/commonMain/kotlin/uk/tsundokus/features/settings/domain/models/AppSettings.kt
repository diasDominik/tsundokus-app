package uk.tsundokus.features.settings.domain.models

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode

/**
 * User-facing application settings. [theme] is persisted locally (so the UI can react instantly)
 * and mirrored to the server; [currency] is server-owned.
 */
data class AppSettings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val currency: AppCurrency = AppCurrency.EUR,
)
