package uk.tsundokus.core.presentation.date

import platform.Foundation.NSCalendar
import platform.Foundation.NSDateComponents
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterMediumStyle
import platform.Foundation.NSDateFormatterNoStyle

actual fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String {
    val components =
        NSDateComponents().apply {
            setYear(year.toLong())
            setMonth(month.toLong())
            setDay(dayOfMonth.toLong())
        }
    val date = NSCalendar.currentCalendar.dateFromComponents(components) ?: return ""
    val formatter =
        NSDateFormatter().apply {
            dateStyle = NSDateFormatterMediumStyle
            timeStyle = NSDateFormatterNoStyle
        }
    return formatter.stringFromDate(date)
}
