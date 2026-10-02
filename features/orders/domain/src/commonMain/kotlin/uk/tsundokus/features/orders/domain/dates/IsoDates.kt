package uk.tsundokus.features.orders.domain.dates

import kotlin.time.Clock

// Dates are stored and compared as ISO `yyyy-MM-dd` strings, which sort lexicographically in
// chronological order, so `<` / `>` work directly. These do the calendar maths on them.

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

/** Today as an ISO `yyyy-MM-dd` string (UTC), used for "releases/expected" comparisons. */
fun todayIso(): String = isoFromEpochMillis(Clock.System.now().toEpochMilliseconds())

/**
 * ISO `yyyy-MM-dd` for a UTC epoch-millis instant — the form the Material date picker hands back.
 */
fun isoFromEpochMillis(millis: Long): String = isoFromEpochDay(millis.floorDiv(MILLIS_PER_DAY))

/**
 * UTC epoch millis for midnight on an ISO `yyyy-MM-dd` date, or null when the string is blank or
 * malformed. Used to seed the Material date picker from a stored date.
 */
fun epochMillisFromIso(iso: String): Long? {
    val parts = iso.split("-")
    if (parts.size != 3) return null
    val year = parts[0].toLongOrNull() ?: return null
    val month = parts[1].toIntOrNull() ?: return null
    val day = parts[2].toIntOrNull() ?: return null
    if (month !in 1..12 || day !in 1..31) return null
    return epochDayFromIso(year, month, day) * MILLIS_PER_DAY
}

/**
 * [iso] moved by [days] (negative goes back), or null when [iso] is not a real date. Checked with
 * [parseIsoDate] first: [epochMillisFromIso] alone would read "2026-02-29" as 1 March.
 */
fun isoPlusDays(
    iso: String,
    days: Int,
): String? {
    if (parseIsoDate(iso) == null) return null
    return epochMillisFromIso(iso)?.let { isoFromEpochMillis(it + days * MILLIS_PER_DAY) }
}

/** Current epoch milliseconds — the [uk.tsundokus.features.orders.domain.models.Order] RECENT sort key. */
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

private const val MILLIS_PER_DAY = 86_400_000L

// Howard Hinnant's days-from-civil algorithm (y/m/d -> days since 1970-01-01).
private fun epochDayFromIso(
    year: Long,
    month: Int,
    day: Int,
): Long {
    val y = if (month <= 2) year - 1 else year
    val era = (if (y >= 0) y else y - 399) / 400
    val yoe = y - era * 400
    val mp = if (month > 2) month - 3 else month + 9
    val doy = (153 * mp + 2) / 5 + day - 1
    val doe = yoe * 365 + yoe / 4 - yoe / 100 + doy
    return era * 146_097 + doe - 719_468
}

// Howard Hinnant's civil-from-days algorithm (days since 1970-01-01 -> y/m/d).
private fun isoFromEpochDay(epochDay: Long): String {
    val z = epochDay + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1_460 + doe / 36_524 - doe / 146_096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val day = (doy - (153 * mp + 2) / 5 + 1).toInt()
    val month = (if (mp < 10) mp + 3 else mp - 9).toInt()
    val year = if (month <= 2) y + 1 else y
    return "${pad4(year)}-${pad2(month)}-${pad2(day)}"
}

private fun pad2(value: Int): String = value.toString().padStart(2, '0')

private fun pad4(value: Long): String = value.toString().padStart(4, '0')
