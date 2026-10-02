package uk.tsundokus.core.presentation.date

/**
 * Formats a calendar date in the viewer's locale using the platform's medium style — `7 Mar 2026`
 * in en-GB, `Mar 7, 2026` in en-US, `2026年3月7日` in ja.
 *
 * Field order, separators and month naming all vary by locale, so this cannot be a shared pattern
 * string with translated month names; it has to come from the platform's own date formatter.
 */
expect fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String

/**
 * A month and its year in the viewer's locale, the month abbreviated: `Mar 2026` in en, `März 2026`
 * in de, `2026年3月` in ja. [month] is 1–12.
 */
expect fun formatShortMonthYear(
    year: Int,
    month: Int,
): String

/**
 * A month in the viewer's locale in its narrowest form, for chart axes: `M` in en, `M` in de, `3` in
 * ja. Several months can share a letter; the axis order tells them apart. [month] is 1–12.
 */
expect fun formatNarrowMonth(month: Int): String
