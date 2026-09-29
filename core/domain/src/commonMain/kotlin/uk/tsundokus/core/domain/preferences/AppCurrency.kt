package uk.tsundokus.core.domain.preferences

/**
 * The currency an amount is in. Amounts are never converted: an order keeps the currency it was
 * placed in. The server stores and transmits [code] (ISO 4217).
 *
 * [symbol] is unambiguous across every currency the app knows ("$" is only ever the US dollar);
 * where no symbol is, it is the code. [decimals] is how many minor-unit digits the currency has —
 * none for the yen, two for most. [symbolAfterAmount] is where the currency's own countries write it:
 * "12.99 €" but "$12.99".
 */
data class AppCurrency(
    val code: String,
    val symbol: String,
    val decimals: Int,
    val displayName: String,
    val symbolAfterAmount: Boolean = false,
) {
    /**
     * [amount] (already formatted) with the symbol where this currency puts it: "12.99 €", "$12.99",
     * "¥1200". A code in front needs a space too: "CHF 12.00".
     */
    fun format(amount: String): String =
        when {
            symbolAfterAmount -> "$amount $symbol"
            symbol.last().isLetter() -> "$symbol $amount"
            else -> "$symbol$amount"
        }

    /** Whether [query] (a code, name or symbol, any case) picks this currency out. */
    fun matches(query: String): Boolean {
        val needle = query.trim()
        if (needle.isEmpty()) return true
        return code.contains(needle, ignoreCase = true) ||
            displayName.contains(needle, ignoreCase = true) ||
            symbol.contains(needle, ignoreCase = true)
    }

    companion object {
        val EUR: AppCurrency get() = CurrencyCatalog.byCode.getValue("EUR")
        val USD: AppCurrency get() = CurrencyCatalog.byCode.getValue("USD")
        val GBP: AppCurrency get() = CurrencyCatalog.byCode.getValue("GBP")

        /** Every currency in use today, by code. */
        val all: List<AppCurrency> get() = CurrencyCatalog.all

        /**
         * The currency for [code]. Blank means "not set" and gives the euro, the app's long-standing
         * default. A code the catalogue does not know is kept as it is rather than replaced, so an
         * order in it still saves back in it.
         */
        fun fromCode(code: String?): AppCurrency {
            val normalized = code?.trim()?.uppercase().orEmpty()
            if (normalized.isEmpty()) return EUR
            return CurrencyCatalog.byCode[normalized] ?: AppCurrency(normalized, normalized, 2, normalized)
        }

        /** The currency used in [region] (ISO 3166 country code, like "DE"), if the app knows it. */
        fun forRegion(region: String?): AppCurrency? = region?.uppercase()?.let(CurrencyCatalog.byRegion::get)
    }
}
