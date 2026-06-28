package uk.tsundokus.features.settings.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
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
 * Theme lives in the local [AppPreferencesRepository] so the app reacts to changes instantly; the
 * server-owned currency flag is cached in [remoteState] and hydrated by [fetch]. [observe] merges
 * both into a single [AppSettings] stream.
 */
@Single(binds = [SettingsRepository::class])
class DefaultSettingsRepository(
    private val settingsService: SettingsService,
    private val appPreferencesRepository: AppPreferencesRepository,
) : SettingsRepository {
    private val remoteState = MutableStateFlow(RemoteSettings())

    override fun observe(): Flow<AppSettings> =
        combine(appPreferencesRepository.themeMode(), remoteState) { theme, remote ->
            AppSettings(
                theme = theme,
                currency = remote.currency,
            )
        }

    override suspend fun fetch(): EmptyResult<DataError.Remote> =
        settingsService
            .getSettings()
            .onSuccess { settings ->
                appPreferencesRepository.setThemeMode(settings.theme)
                cacheRemote(settings)
            }.asEmptyResult()

    override suspend fun updateTheme(theme: ThemeMode): EmptyResult<DataError.Remote> {
        // Persist locally first so the UI flips immediately even if the network call is slow/fails.
        appPreferencesRepository.setThemeMode(theme)
        return settingsService
            .updateSettings(theme = theme)
            .onSuccess { cacheRemote(it) }
            .asEmptyResult()
    }

    override suspend fun updateCurrency(currency: AppCurrency): EmptyResult<DataError.Remote> =
        settingsService
            .updateSettings(currency = currency)
            .onSuccess { cacheRemote(it) }
            .asEmptyResult()

    private fun cacheRemote(settings: AppSettings) {
        remoteState.update {
            it.copy(
                currency = settings.currency,
            )
        }
    }

    private data class RemoteSettings(
        val currency: AppCurrency = AppCurrency.EUR,
    )
}
