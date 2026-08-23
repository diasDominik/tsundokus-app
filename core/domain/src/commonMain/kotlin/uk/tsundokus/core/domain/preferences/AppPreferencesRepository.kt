package uk.tsundokus.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/**
 * Local, device-scoped mirror of the user's preferences (theme, currency).
 *
 * Theme is owned here outright. Currency is owned by the server, but is mirrored locally so every
 * feature can read the user's choice without depending on the settings feature — and so the choice
 * survives a restart instead of falling back to a default until the first settings fetch lands.
 */
interface AppPreferencesRepository {
    fun themeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)

    fun currency(): Flow<AppCurrency>

    suspend fun setCurrency(currency: AppCurrency)
}
