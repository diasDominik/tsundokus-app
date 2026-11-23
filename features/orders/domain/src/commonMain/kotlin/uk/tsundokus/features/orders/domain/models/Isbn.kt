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

    // What OCR gives for the digits of a number: full-width digits on Japanese pages, and the letters
    // a digit is most often misread as. An X may end a part, as an ISBN-10 check digit.
    private const val DIGIT = "[0-9０-９OoIl|]"
    private const val PART = "$DIGIT+[Xx]?"

    // Hyphens as typeset: ASCII, Unicode hyphen and dashes, minus sign, full-width hyphen-minus.
    private const val HYPHEN = "[-‐-—−－]"

    // Plain, no-break, thin and narrow no-break spaces; one between two parts of a number.
    private val SPACE = Regex("[    ]")

    // Parts joined by single hyphens; after a hyphen the X check digit may stand alone ("0-439-42089-X").
    private const val HYPHENATED = "$PART(?:$HYPHEN(?:$PART|[Xx]))*"

    /** A number as printed: hyphenated stretches joined by single spaces. */
    private val NUMBER_RUN = Regex("$HYPHENATED(?:${SPACE.pattern}$HYPHENATED)*")
    private val FULL_WIDTH_DIGITS = '０'..'９'

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
     * Every ISBN in [text], as ISBN-13 digits, in the order they first appear and without repeats.
     * Meant for text read off a picture: an order page lists several books, and a product page shows
     * a book's ISBN-10 beside its ISBN-13.
     *
     * An ISBN's digits may be split by hyphens or single spaces, and a number may run straight into
     * the next one ("9784088820453 1,320 yen"), so each space-separated stretch of a number is tried
     * alone and with its neighbours. OCR's usual misreads inside a number are undone first (O for 0,
     * I or l for 1, full-width digits), and the check digit must still be right, so a misread that
     * gets past that is dropped rather than turned into some other book.
     */
    fun findAll(text: String): List<String> =
        NUMBER_RUN
            .findAll(text)
            .flatMap { run -> isbnsIn(run.value.split(SPACE).map(::readOcrDigits)) }
            .distinct()
            .toList()

    /**
     * The ISBNs in one number's space-separated [parts]. From each part, the longest stretch that is
     * a valid ISBN is taken (an ISBN-13 before an ISBN-10) and its parts skipped, so the tail of one
     * ISBN is never read again as the start of another.
     */
    private fun isbnsIn(parts: List<String>): List<String> {
        val found = mutableListOf<String>()
        var start = 0
        while (start < parts.size) {
            var digits = ""
            var best: Pair<String, Int>? = null
            for (end in start until parts.size) {
                digits += parts[end]
                if (digits.length > ISBN_13_LENGTH) break
                normalize(digits)?.let { best = it to end }
            }
            val match = best
            if (match == null) {
                start++
            } else {
                found += match.first
                start = match.second + 1
            }
        }
        return found
    }

    /** One part of a number as plain ASCII digits: hyphens dropped, look-alike letters undone. */
    private fun readOcrDigits(part: String): String =
        buildString {
            for (char in part) {
                when (char) {
                    in '0'..'9', 'X', 'x' -> append(char)
                    in FULL_WIDTH_DIGITS -> append('0' + (char - FULL_WIDTH_DIGITS.first))
                    'O', 'o' -> append('0')
                    'I', 'l', '|' -> append('1')
                    else -> Unit // a hyphen of some kind
                }
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
