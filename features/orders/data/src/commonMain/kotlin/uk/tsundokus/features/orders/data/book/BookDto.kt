package uk.tsundokus.features.orders.data.book

import kotlinx.serialization.Serializable

@Serializable
data class BookDto(
    val isbn: String,
    val title: String? = null,
    val author: String? = null,
    val publisher: String? = null,
    val volume: String? = null,
    val releaseDate: String? = null,
    val hasCover: Boolean = false,
)
