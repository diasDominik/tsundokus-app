package uk.tsundokus.core.data.preferences

import kotlinx.browser.window
import uk.tsundokus.core.domain.preferences.AppCurrency

/**
 * Browsers expose no default currency, but they do expose the preferred language, which usually
 * carries a region: "de-DE" is Germany, so the euro. A bare "de" says nothing, and gives null.
 */
internal actual fun deviceCurrencyCode(): String? {
    val region =
        window.navigator.language
            .split('-', '_')
            .getOrNull(1)
    return AppCurrency.forRegion(region)?.code
}
