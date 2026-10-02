package uk.tsundokus.core.presentation.date

import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle

actual fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String {
    val date = dateOf(year, month, dayOfMonth) ?: return ""
    val formatter =
        NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterNoStyle
        }
    return formatter.stringFromDate(date)
}

actual fun formatShortMonthYear(
    year: Int,
    month: Int,
): String {
    val date = dateOf(year, month, 1) ?: return ""
    val formatter = NSDateFormatter().apply { setLocalizedDateFormatFromTemplate("yMMM") }
    return formatter.stringFromDate(date)
}

actual fun formatNarrowMonth(month: Int): String =
    NSDateFormatter().veryShortStandaloneMonthSymbols.getOrNull(month - 1) as? String ?: ""

private fun dateOf(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): NSDate? {
    val components =
        NSDateComponents().apply {
            setYear(year.toLong())
            setMonth(month.toLong())
            setDay(dayOfMonth.toLong())
        }
    return NSCalendar.currentCalendar.dateFromComponents(components)
}
