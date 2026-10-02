package uk.tsundokus.core.presentation.date

import android.text.format.DateFormat
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

actual fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String =
    LocalDate
        .of(year, month, dayOfMonth)
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

// java.time's own localized patterns arrived in API 35; ICU's best pattern for the skeleton works
// on every supported version.
actual fun formatShortMonthYear(
    year: Int,
    month: Int,
): String {
    val locale = Locale.getDefault()
    val pattern = DateFormat.getBestDateTimePattern(locale, "yMMM")
    return YearMonth.of(year, month).format(DateTimeFormatter.ofPattern(pattern, locale))
}

actual fun formatNarrowMonth(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.NARROW, Locale.getDefault())
