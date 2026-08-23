package uk.tsundokus.core.data.preferences

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode

@Single(binds = [AppPreferencesRepository::class])
class KSafeAppPreferencesRepository(
    @Named("prefs") private val prefs: KSafe,
) : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> =
        prefs.getFlow(KEY_THEME_MODE, ThemeMode.SYSTEM.name).map { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.put(KEY_THEME_MODE, mode.name, KSafeWriteMode.Plain)
    }

    override fun currency(): Flow<AppCurrency> =
        prefs.getFlow(KEY_CURRENCY, AppCurrency.EUR.name).map { stored ->
            AppCurrency.fromName(stored)
        }

    override suspend fun setCurrency(currency: AppCurrency) {
        prefs.put(KEY_CURRENCY, currency.name, KSafeWriteMode.Plain)
    }

    private companion object {
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_CURRENCY = "currency"
    }
}
