package uk.tsundokus.core.domain.preferences

import kotlinx.coroutines.flow.Flow

/** Local, device-scoped user preferences (theme). Not synced to the server. */
interface AppPreferencesRepository {
    fun themeMode(): Flow<ThemeMode>

    suspend fun setThemeMode(mode: ThemeMode)
}
