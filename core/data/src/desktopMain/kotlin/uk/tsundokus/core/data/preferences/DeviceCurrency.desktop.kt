package uk.tsundokus.core.data.preferences

import java.util.Currency
import java.util.Locale

internal actual fun deviceCurrencyCode(): String? =
    runCatching { Currency.getInstance(Locale.getDefault()).currencyCode }.getOrNull()
