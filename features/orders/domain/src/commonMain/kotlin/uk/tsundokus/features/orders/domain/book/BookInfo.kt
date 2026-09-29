package uk.tsundokus.features.orders.domain.book

/**
 * What the book databases know about an ISBN. Any field can be missing. [volume] is the number alone
 * ("12"); [releaseDate] is ISO `yyyy-MM-dd`.
 */
data class BookInfo(
    val isbn: String,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val volume: String? = null,
    val releaseDate: String? = null,
    val hasCover: Boolean = false,
)
