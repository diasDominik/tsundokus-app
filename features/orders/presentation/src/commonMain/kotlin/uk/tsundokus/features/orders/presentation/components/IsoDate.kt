package uk.tsundokus.features.orders.presentation.components

/** The parts of an ISO `yyyy-MM-dd` date, as a platform scheduler needs them. */
data class IsoDate(
    val year: Int,
    val month: Int,
    val day: Int,
)

/** Null for anything that is not a real calendar date — "2026-02-30" included. */
fun parseIsoDate(iso: String): IsoDate? {
    val parts = iso.split('-')
    if (parts.size != 3 || parts[0].length != 4) return null
    val (year, month, day) = parts.map { it.toIntOrNull() ?: return null }
    if (month !in 1..12 || day !in 1..daysIn(year, month)) return null
    return IsoDate(year, month, day)
}

private fun daysIn(
    year: Int,
    month: Int,
): Int =
    when (month) {
        2 -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28
        4, 6, 9, 11 -> 30
        else -> 31
    }
