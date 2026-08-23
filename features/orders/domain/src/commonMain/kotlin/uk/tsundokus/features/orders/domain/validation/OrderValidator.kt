package uk.tsundokus.features.orders.domain.validation

object OrderValidator {
    private const val MAX_DECIMALS = 2

    fun isTitleValid(title: String): Boolean = title.isNotBlank()

    /**
     * Reduces raw price input to what can actually parse as an amount: ASCII digits and at most one
     * decimal separator, capped at [MAX_DECIMALS] places. A comma is accepted and normalised to a
     * dot, since that is what a decimal keypad emits in many locales.
     *
     * A keyboard type is only a hint — a physical keyboard can type anything into the field — so
     * filtering here is what actually keeps the value numeric.
     */
    fun sanitizePrice(input: String): String {
        val builder = StringBuilder()
        var seenSeparator = false
        var decimals = 0
        for (char in input) {
            when {
                // Not Char.isDigit(): that accepts non-ASCII digits, which cannot be parsed back.
                char in '0'..'9' -> {
                    if (seenSeparator) {
                        if (decimals == MAX_DECIMALS) continue
                        decimals++
                    }
                    builder.append(char)
                }

                (char == '.' || char == ',') && !seenSeparator && builder.isNotEmpty() -> {
                    seenSeparator = true
                    builder.append('.')
                }
            }
        }
        return builder.toString()
    }
}
