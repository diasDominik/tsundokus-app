package uk.tsundokus.features.orders.domain.models

/** What a scanned ISBN means for the user's orders. */
sealed interface IsbnMatch {
    /** Exactly one order with this ISBN is still on its way: that is the one that arrived. */
    data class Receivable(val order: Order) : IsbnMatch

    /** More than one open order carries this ISBN (it was ordered twice); the user picks. */
    data class Ambiguous(val orders: List<Order>) : IsbnMatch

    /** Every order with this ISBN has already been received or cancelled. */
    data class AlreadyReceived(val order: Order) : IsbnMatch

    /**
     * No order carries this ISBN. [candidates] are the open orders that have no ISBN yet — the
     * ones it could belong to, from before ISBNs were recorded.
     */
    data class NoMatch(val candidates: List<Order>) : IsbnMatch

    companion object {
        /** Statuses that mean the volume is not in hand yet, so a scan can still receive it. */
        private val OPEN_STATUSES = setOf(OrderStatus.ORDERED, OrderStatus.SHIPPED, OrderStatus.DELAYED)

        /** Matches [isbn], already normalised to ISBN-13 digits, against [orders]. */
        fun forIsbn(
            isbn: String,
            orders: List<Order>,
        ): IsbnMatch {
            val withIsbn = orders.filter { it.isbn == isbn }
            val open = withIsbn.filter { it.status in OPEN_STATUSES }
            return when {
                open.size == 1 -> {
                    Receivable(open.single())
                }

                open.size > 1 -> {
                    Ambiguous(open.sortedBy(Order::createdAt))
                }

                withIsbn.isNotEmpty() -> {
                    AlreadyReceived(withIsbn.maxBy(Order::createdAt))
                }

                else -> {
                    NoMatch(
                        orders.filter { it.isbn.isBlank() && it.status in OPEN_STATUSES }.sortedBy(Order::title),
                    )
                }
            }
        }
    }
}
