package uk.tsundokus.core.presentation.date

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

actual fun formatShortMonthYear(
    year: Int,
    month: Int,
): String = YearMonth.of(year, month).format(DateTimeFormatter.ofLocalizedPattern("yMMM"))

actual fun formatNarrowMonth(month: Int): String =
    Month.of(month).getDisplayName(TextStyle.NARROW, Locale.getDefault(Locale.Category.FORMAT))
