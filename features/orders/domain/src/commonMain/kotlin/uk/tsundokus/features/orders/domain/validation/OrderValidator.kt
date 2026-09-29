package uk.tsundokus.features.orders.domain.validation

import uk.tsundokus.features.orders.domain.models.Isbn

object OrderValidator {
    private const val DEFAULT_DECIMALS = 2

    fun isTitleValid(title: String): Boolean = title.isNotBlank()

    /** A required free-text field (author, publisher, store) must carry something. */
    fun isRequiredTextValid(value: String): Boolean = value.isNotBlank()

    /** A required ISBN must be a real one: ISBN-13 or ISBN-10 with a correct check digit. */
    fun isIsbnValid(isbn: String): Boolean = Isbn.normalize(isbn) != null

    /** A required date must be set; the picker guarantees the format, so presence is enough. */
    fun isRequiredDateValid(isoDate: String): Boolean = isoDate.isNotBlank()

    /**
     * Whether [isoDate] falls before [floor], for two dates the picker produced.
     *
     * ISO `yyyy-MM-dd` is zero-padded and fixed-width, so a plain string comparison orders these
     * correctly and no date library is needed. A blank on either side is "not known yet", never a
     * violation — an order can be shipped before its arrival is recorded.
     */
    fun isBefore(
        isoDate: String,
        floor: String,
    ): Boolean = isoDate.isNotBlank() && floor.isNotBlank() && isoDate < floor

    /**
     * A required price must parse as a non-negative amount. Zero is allowed — a free or gifted
     * volume is still a real price — but blank or unparseable is not.
     */
    fun isPriceValid(price: String): Boolean {
        val amount = price.toDoubleOrNull() ?: return false
        return amount >= 0.0
    }

    /**
     * Reduces raw price input to what can actually parse as an amount: ASCII digits and at most one
     * decimal separator, capped at [maxDecimals] places — the currency's own: two for most, three for
     * a few. A comma is accepted and normalised to a dot, since that is what a decimal keypad emits in
     * many locales. For a currency without decimals, like the yen, a separator can only be grouping —
     * "1,200" as a Japanese shop prints it — so it is skipped and the digits kept.
     *
     * A keyboard type is only a hint — a physical keyboard can type anything into the field — so
     * filtering here is what actually keeps the value numeric.
     */
    fun sanitizePrice(
        input: String,
        maxDecimals: Int = DEFAULT_DECIMALS,
    ): String {
        val builder = StringBuilder()
        var seenSeparator = false
        var decimals = 0
        for (char in input) {
            when {
                // Not Char.isDigit(): that accepts non-ASCII digits, which cannot be parsed back.
                char in '0'..'9' -> {
                    if (seenSeparator) {
                        if (decimals == maxDecimals) continue
                        decimals++
                    }
                    builder.append(char)
                }

                (char == '.' || char == ',') && !seenSeparator && builder.isNotEmpty() && maxDecimals > 0 -> {
                    // (With no decimals the separator falls through and is skipped as grouping.)
                    seenSeparator = true
                    builder.append('.')
                }
            }
        }
        return builder.toString()
    }
}
