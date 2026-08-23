package uk.tsundokus.features.settings.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.asEmptyResult
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.settings.domain.models.AppSettings
import uk.tsundokus.features.settings.domain.settings.SettingsRepository
import uk.tsundokus.features.settings.domain.settings.SettingsService

/**
 * Both settings live in the local [AppPreferencesRepository] so the app reacts to a change
 * instantly, and so a restart shows the user's own choice rather than a default until the first
 * [fetch] lands. Currency stays server-owned — [fetch] overwrites the local mirror with whatever
 * the server holds — but every feature can read it without depending on this one.
 */
@Single(binds = [SettingsRepository::class])
class DefaultSettingsRepository(
    private val settingsService: SettingsService,
    private val appPreferencesRepository: AppPreferencesRepository,
) : SettingsRepository {
    override fun observe(): Flow<AppSettings> =
        combine(
            appPreferencesRepository.themeMode(),
            appPreferencesRepository.currency(),
        ) { theme, currency ->
            AppSettings(
                theme = theme,
                currency = currency,
            )
        }

    override suspend fun fetch(): EmptyResult<DataError.Remote> =
        settingsService
            .getSettings()
            .onSuccess { settings ->
                appPreferencesRepository.setThemeMode(settings.theme)
                appPreferencesRepository.setCurrency(settings.currency)
            }.asEmptyResult()

    override suspend fun updateTheme(theme: ThemeMode): EmptyResult<DataError.Remote> {
        // Persist locally first so the UI flips immediately even if the network call is slow/fails.
        appPreferencesRepository.setThemeMode(theme)
        return settingsService
            .updateSettings(theme = theme)
            .onSuccess { appPreferencesRepository.setCurrency(it.currency) }
            .asEmptyResult()
    }

    override suspend fun updateCurrency(currency: AppCurrency): EmptyResult<DataError.Remote> {
        // Same as the theme: write locally first so the choice is visible (and usable by the order
        // form) even while the server round-trip is in flight.
        appPreferencesRepository.setCurrency(currency)
        return settingsService
            .updateSettings(currency = currency)
            .onSuccess { appPreferencesRepository.setCurrency(it.currency) }
            .asEmptyResult()
    }
}
