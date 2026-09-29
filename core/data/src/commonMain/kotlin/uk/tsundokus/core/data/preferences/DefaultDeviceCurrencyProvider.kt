package uk.tsundokus.core.data.preferences

import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.DeviceCurrencyProvider

@Single(binds = [DeviceCurrencyProvider::class])
class DefaultDeviceCurrencyProvider : DeviceCurrencyProvider {
    override fun currentCurrency(): AppCurrency? = deviceCurrencyCode()?.let(AppCurrency::fromCode)
}
