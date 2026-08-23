package uk.tsundokus.features.orders.domain.models

enum class OrderSort {
    RECENT,
    RELEASE,
    TITLE,
    PRICE,
    ;

    fun next(): OrderSort = entries[(ordinal + 1) % entries.size]
}
