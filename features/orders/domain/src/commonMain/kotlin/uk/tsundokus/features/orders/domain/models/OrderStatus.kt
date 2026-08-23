package uk.tsundokus.features.orders.domain.models

enum class OrderStatus {
    ORDERED,
    SHIPPED,
    DELAYED,
    RECEIVED,
    CANCELLED,
    ;

    companion object {
        fun fromName(name: String?): OrderStatus = entries.firstOrNull { it.name == name } ?: ORDERED

        /** Display grouping order used by the Orders list when no single-status filter is active. */
        val groupOrder: List<OrderStatus> = listOf(DELAYED, SHIPPED, ORDERED, RECEIVED, CANCELLED)
    }
}
