package uk.tsundokus.features.settings.domain.settings

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.settings.domain.models.AppSettings

/** Remote API for settings. Implemented in :features:settings:data by KtorSettingsService. */
interface SettingsService {
    suspend fun getSettings(): Result<AppSettings, DataError.Remote>

    suspend fun updateSettings(
        theme: ThemeMode? = null,
        currency: AppCurrency? = null,
    ): Result<AppSettings, DataError.Remote>
}
