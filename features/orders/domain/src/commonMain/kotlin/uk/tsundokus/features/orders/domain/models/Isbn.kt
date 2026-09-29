package uk.tsundokus.features.orders.domain.models

/**
 * ISBN parsing. Every ISBN is kept as the 13 digits of its ISBN-13 form, so the same book matches
 * whether it was typed with hyphens, typed as an old ISBN-10, or scanned from the EAN-13 barcode.
 * Mirrors the server's parser, which rejects anything this one would.
 */
object Isbn {
    private const val ISBN_10_LENGTH = 10
    private const val ISBN_13_LENGTH = 13
    private val BOOKLAND_PREFIXES = setOf("978", "979")

    /**
     * The ISBN-13 digits for [raw], or null when it is not a valid ISBN. Accepts ISBN-13 or ISBN-10,
     * with or without hyphens and spaces; the check digit must be correct.
     *
     * The 978/979 prefix check is what tells a book's ISBN barcode apart from the second barcode a
     * Japanese volume carries under it — a valid EAN-13 too, but a price and category code.
     */
    fun normalize(raw: String): String? {
        val compact = raw.filterNot { it == '-' || it == ' ' }.uppercase()
        return when (compact.length) {
            ISBN_13_LENGTH -> compact.takeIf(::isValidIsbn13)
            ISBN_10_LENGTH -> compact.takeIf(::isValidIsbn10)?.let(::isbn10To13)
            else -> null
        }
    }

    /**
     * Reduces raw input to what an ISBN can contain: digits, plus an X where an ISBN-10 check digit
     * may be one. Hyphens and spaces are dropped rather than kept, since the stored form has none.
     */
    fun sanitize(input: String): String =
        input
            .uppercase()
            .filter { it in '0'..'9' || it == 'X' }
            .take(ISBN_13_LENGTH)

    private fun isValidIsbn13(value: String): Boolean =
        value.all { it in '0'..'9' } &&
            value.take(3) in BOOKLAND_PREFIXES &&
            ean13CheckDigit(value.take(ISBN_13_LENGTH - 1)) == value.last().digitToInt()

    private fun isValidIsbn10(value: String): Boolean {
        val body = value.take(ISBN_10_LENGTH - 1)
        val check = value.last()
        if (!body.all { it in '0'..'9' } || !(check in '0'..'9' || check == 'X')) return false
        val sum =
            body.mapIndexed { index, digit -> (ISBN_10_LENGTH - index) * digit.digitToInt() }.sum() +
                if (check == 'X') 10 else check.digitToInt()
        return sum % 11 == 0
    }

    private fun isbn10To13(isbn10: String): String {
        val body = "978" + isbn10.take(ISBN_10_LENGTH - 1)
        return body + ean13CheckDigit(body)
    }

    /** The EAN-13 check digit over the first twelve digits: weights alternate 1 and 3. */
    private fun ean13CheckDigit(twelveDigits: String): Int {
        val sum =
            twelveDigits.withIndex().sumOf { (index, digit) ->
                val weight = if (index % 2 == 0) 1 else 3
                digit.digitToInt() * weight
            }
        return (10 - sum % 10) % 10
    }
}
