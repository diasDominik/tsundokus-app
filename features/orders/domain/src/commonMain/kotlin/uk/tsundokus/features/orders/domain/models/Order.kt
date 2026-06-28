package uk.tsundokus.features.orders.domain.models

import uk.tsundokus.core.domain.preferences.AppCurrency

/**
 * A tracked manga order. Dates are ISO `yyyy-MM-dd` strings (empty = unset) to mirror the design
 * and avoid a datetime dependency; [createdAt] is epoch milliseconds and is the RECENT sort key.
 */
data class Order(
    val id: String,
    val title: String,
    val author: String = "",
    val publisher: String = "",
    val volume: String = "",
    val store: String = "",
    val price: Double = 0.0,
    val currency: AppCurrency = AppCurrency.EUR,
    val status: OrderStatus = OrderStatus.ORDERED,
    val readState: ReadState = ReadState.WANT,
    val orderDate: String = "",
    val releaseDate: String = "",
    val shipDate: String = "",
    val eta: String = "",
    val receivedDate: String = "",
    val delayedTo: String = "",
    val createdAt: Long = 0L,
) {
    val subtitle: String
        get() = listOf(author, volume).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { publisher }

    val byline: String
        get() = listOf(author, publisher).filter { it.isNotBlank() }.joinToString(" · ")
}
