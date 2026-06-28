package uk.tsundokus.core.domain.preferences

/**
 * Supported display currencies. [symbol] is what the UI prints in front of an amount.
 * The server persists/transmits the enum name (EUR/USD/GBP).
 */
enum class AppCurrency(val symbol: String) {
    EUR("€"),
    USD("$"),
    GBP("£"),
    ;

    companion object {
        fun fromName(name: String?): AppCurrency = entries.firstOrNull { it.name == name } ?: EUR

        fun fromSymbol(symbol: String?): AppCurrency = entries.firstOrNull { it.symbol == symbol } ?: EUR
    }
}
