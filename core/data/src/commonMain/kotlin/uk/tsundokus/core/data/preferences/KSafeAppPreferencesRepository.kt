package uk.tsundokus.core.data.preferences

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.DeviceCurrencyProvider
import uk.tsundokus.core.domain.preferences.ThemeMode

@Single(binds = [AppPreferencesRepository::class])
class KSafeAppPreferencesRepository(
    @Named("prefs") private val prefs: KSafe,
    private val deviceCurrencyProvider: DeviceCurrencyProvider,
) : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> =
        prefs.getFlow(KEY_THEME_MODE, ThemeMode.SYSTEM.name).map { stored ->
            runCatching { ThemeMode.valueOf(stored) }.getOrDefault(ThemeMode.SYSTEM)
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        prefs.put(KEY_THEME_MODE, mode.name, KSafeWriteMode.Plain)
    }

    /**
     * The stored ISO code. Before anything is stored — a fresh install, before the first settings
     * sync — it is the device's currency, so the first order is already in the right one.
     */
    override fun currency(): Flow<AppCurrency> =
        prefs.getFlow(KEY_CURRENCY, "").map { stored ->
            if (stored.isBlank()) {
                deviceCurrencyProvider.currentCurrency() ?: AppCurrency.EUR
            } else {
                AppCurrency.fromCode(stored)
            }
        }

    override suspend fun setCurrency(currency: AppCurrency) {
        prefs.put(KEY_CURRENCY, currency.code, KSafeWriteMode.Plain)
    }

    private companion object {
        private const val KEY_THEME_MODE = "themeMode"
        private const val KEY_CURRENCY = "currency"
    }
}
