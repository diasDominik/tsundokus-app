package uk.tsundokus.features.orders.domain.models

enum class OrderSort(val label: String) {
    RECENT("Recent"),
    RELEASE("Release"),
    TITLE("Title"),
    PRICE("Price"),
    ;

    fun next(): OrderSort = entries[(ordinal + 1) % entries.size]
}
