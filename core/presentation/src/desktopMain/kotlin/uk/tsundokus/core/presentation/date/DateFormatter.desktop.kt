package uk.tsundokus.core.presentation.date

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

actual fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String =
    LocalDate
        .of(year, month, dayOfMonth)
        .format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
