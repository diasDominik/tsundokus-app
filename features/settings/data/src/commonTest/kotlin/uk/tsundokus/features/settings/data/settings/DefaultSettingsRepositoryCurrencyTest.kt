package uk.tsundokus.features.settings.data.settings

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.DeviceCurrencyProvider
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.settings.domain.models.AppSettings
import uk.tsundokus.features.settings.domain.settings.SettingsService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private class FakeSettingsService(
    var stored: AppSettings,
) : SettingsService {
    var sentCurrency: AppCurrency? = null

    override suspend fun getSettings(): Result<AppSettings, DataError.Remote> = Result.Success(stored)

    override suspend fun updateSettings(
        theme: ThemeMode?,
        currency: AppCurrency?,
    ): Result<AppSettings, DataError.Remote> {
        sentCurrency = currency
        stored = stored.copy(currency = currency ?: stored.currency, isCurrencyChosen = true)
        return Result.Success(stored)
    }
}

private class FakePreferences : AppPreferencesRepository {
    val currency = MutableStateFlow(AppCurrency.EUR)

    override fun themeMode(): Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override fun currency(): Flow<AppCurrency> = currency

    override suspend fun setCurrency(currency: AppCurrency) {
        this.currency.value = currency
    }
}

private class FixedDeviceCurrency(
    private val currency: AppCurrency?,
) : DeviceCurrencyProvider {
    override fun currentCurrency(): AppCurrency? = currency
}

class DefaultSettingsRepositoryCurrencyTest {
    private val yen = AppCurrency.fromCode("JPY")

    @Test
    fun `a new account takes the device's currency and tells the server`() =
        runTest {
            val service = FakeSettingsService(AppSettings(currency = AppCurrency.EUR, isCurrencyChosen = false))
            val prefs = FakePreferences()
            val sut = DefaultSettingsRepository(service, prefs, FixedDeviceCurrency(yen))

            sut.fetch()

            assertEquals(yen, prefs.currency.first())
            assertEquals(yen, service.sentCurrency)
        }

    @Test
    fun `a chosen currency is kept whatever the device says`() =
        runTest {
            val service = FakeSettingsService(AppSettings(currency = AppCurrency.GBP, isCurrencyChosen = true))
            val prefs = FakePreferences()
            val sut = DefaultSettingsRepository(service, prefs, FixedDeviceCurrency(yen))

            sut.fetch()

            assertEquals(AppCurrency.GBP, prefs.currency.first())
            assertNull(service.sentCurrency)
        }

    @Test
    fun `a device that cannot tell leaves the stand-in and settles it`() =
        runTest {
            val service = FakeSettingsService(AppSettings(currency = AppCurrency.EUR, isCurrencyChosen = false))
            val prefs = FakePreferences()
            val sut = DefaultSettingsRepository(service, prefs, FixedDeviceCurrency(null))

            sut.fetch()

            assertEquals(AppCurrency.EUR, prefs.currency.first())
            assertEquals(AppCurrency.EUR, service.sentCurrency)
        }
}
