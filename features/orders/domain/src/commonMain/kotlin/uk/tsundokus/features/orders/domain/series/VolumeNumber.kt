package uk.tsundokus.features.orders.domain.series

import uk.tsundokus.features.orders.domain.models.Order

/** An order's title with any volume number taken off, and that number. */
data class TitledVolume(
    val title: String,
    val number: Int?,
)

/**
 * Reads which volume an order is. Its volume field wins when it holds a number ("Vol. 12", "12",
 * "第12巻"); otherwise the end of the title is read, with the same rules the server uses when it
 * splits looked-up titles:
 * - a marked number always counts: "Naruto, Vol. 12", "進撃の巨人 第3巻", "進撃の巨人（1）";
 * - a bare trailing number ("進撃の巨人 1", "ONE PIECE 107") only for Japanese books (ISBN 978-4) or
 *   orders without an ISBN — so "Fahrenheit 451" with its ISBN stays a title, not volume 451.
 */
object VolumeNumber {
    /** A volume number as it appears in a volume field or title. */
    internal val NUMBER = Regex("""\d{1,4}""")
    private val MARKED =
        Regex("""^(.+?)[\s.,:=　]*(?:vol(?:ume)?\.?\s*|第\s*)(\d{1,4})\s*巻?$""", RegexOption.IGNORE_CASE)
    private val BRACKETED = Regex("""^(.+?)\s*[（(]\s*(\d{1,4})\s*[)）]$""")
    private val TRAILING = Regex("""^(.+?)[\s.,:=　]+(\d{1,4})\s*巻?$""")

    fun parse(order: Order): TitledVolume {
        val fromField = NUMBER.find(order.volume)?.value?.toInt()
        val fromTitle =
            splitTitle(
                order.title.trim(),
                bareNumberAllowed =
                    order.isbn.isBlank() || order.isbn.startsWith("9784"),
            )
        return when {
            fromTitle == null -> TitledVolume(order.title.trim(), fromField)

            // "One Piece 107" with volume "107": the title's number is the volume, not part of the title.
            fromField == null || fromField == fromTitle.number -> TitledVolume(fromTitle.title, fromTitle.number)

            // The field says otherwise: trust it, and leave the title as the user wrote it.
            else -> TitledVolume(order.title.trim(), fromField)
        }
    }

    private fun splitTitle(
        title: String,
        bareNumberAllowed: Boolean,
    ): TitledVolume? {
        val match =
            MARKED.find(title)
                ?: BRACKETED.find(title)
                ?: TRAILING.takeIf { bareNumberAllowed }?.find(title)
                ?: return null
        val (rest, number) = match.destructured
        return TitledVolume(rest.trim(), number.toInt())
    }
}
