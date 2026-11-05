package uk.tsundokus.features.orders.domain.models

private val MOVES_AWAY_FROM_RECEIVED = setOf(OrderStatus.SHIPPED, OrderStatus.CANCELLED)

/** A change that can be made to many orders at once. */
sealed interface OrderChange {
    data class Status(val status: OrderStatus) : OrderChange

    data class Reading(val readState: ReadState) : OrderChange

    /**
     * [order] with this change made, or null when it doesn't apply: the order is already like that, or
     * it can't get there from where it is — a cancelled order is never received, a received one never
     * goes back in transit or gets cancelled. Arriving or shipping fills in [today] when no date is
     * set, as scanning a book in does, so the date shows at once and offline.
     */
    fun applyTo(
        order: Order,
        today: String,
    ): Order? =
        when (this) {
            is Status -> {
                when {
                    order.status == status -> {
                        null
                    }

                    order.status == OrderStatus.CANCELLED && status == OrderStatus.RECEIVED -> {
                        null
                    }

                    order.status == OrderStatus.RECEIVED && status in MOVES_AWAY_FROM_RECEIVED -> {
                        null
                    }

                    status == OrderStatus.RECEIVED -> {
                        order.copy(
                            status = status,
                            receivedDate = order.receivedDate.ifBlank { today },
                        )
                    }

                    status == OrderStatus.SHIPPED -> {
                        order.copy(
                            status = status,
                            shipDate = order.shipDate.ifBlank { today },
                        )
                    }

                    else -> {
                        order.copy(status = status)
                    }
                }
            }

            is Reading -> {
                if (order.readState == readState) null else order.copy(readState = readState)
            }
        }
}
