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
