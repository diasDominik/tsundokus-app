package uk.tsundokus.core.data.preferences

import platform.Foundation.NSLocale
import platform.Foundation.currencyCode
import platform.Foundation.currentLocale

internal actual fun deviceCurrencyCode(): String? = NSLocale.currentLocale.currencyCode
