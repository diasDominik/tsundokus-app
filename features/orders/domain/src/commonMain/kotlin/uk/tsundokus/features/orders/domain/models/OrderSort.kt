package uk.tsundokus.features.orders.domain.models

enum class OrderSort {
    RECENT,
    RELEASE,
    TITLE,
    PRICE,
    ;

    /**
     * The direction that reads as "obvious" for this key, used when the user first picks it:
     * newest and priciest first, but earliest release and A-to-Z titles.
     */
    val defaultDirection: SortDirection
        get() =
            when (this) {
                RECENT, PRICE -> SortDirection.DESCENDING
                RELEASE, TITLE -> SortDirection.ASCENDING
            }
}

enum class SortDirection {
    ASCENDING,
    DESCENDING,
    ;

    fun flipped(): SortDirection = if (this == ASCENDING) DESCENDING else ASCENDING
}
