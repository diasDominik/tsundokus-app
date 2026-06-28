package uk.tsundokus.features.orders.domain.models

enum class ReadState(val label: String, val fullLabel: String) {
    WANT("Want", "Want to read"),
    READING("Reading", "Reading"),
    READ("Read", "Read"),
    ;

    /** Next state in the cycle WANT -> READING -> READ -> WANT. */
    fun next(): ReadState = entries[(ordinal + 1) % entries.size]

    companion object {
        fun fromName(name: String?): ReadState = entries.firstOrNull { it.name == name } ?: WANT

        /** Display grouping order used by the Reading list. */
        val groupOrder: List<ReadState> = listOf(READING, WANT, READ)
    }
}
