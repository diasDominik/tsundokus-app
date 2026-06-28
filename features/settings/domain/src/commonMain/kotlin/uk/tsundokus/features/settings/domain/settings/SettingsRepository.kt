package uk.tsundokus.features.settings.domain.settings

import kotlinx.coroutines.flow.Flow
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.features.settings.domain.models.AppSettings

/**
 * Source of truth for [AppSettings]: combines the locally persisted theme with the server-owned
 * currency flag. Mutations update both the local cache and the remote service.
 */
interface SettingsRepository {
    fun observe(): Flow<AppSettings>

    suspend fun fetch(): EmptyResult<DataError.Remote>

    suspend fun updateTheme(theme: ThemeMode): EmptyResult<DataError.Remote>

    suspend fun updateCurrency(currency: AppCurrency): EmptyResult<DataError.Remote>
}
