@file:OptIn(ExperimentalWasmJsInterop::class)

package uk.tsundokus.core.presentation.date

import kotlin.js.ExperimentalWasmJsInterop

actual fun formatMediumDate(
    year: Int,
    month: Int,
    dayOfMonth: Int,
): String = toLocaleDateString(year, month - 1, dayOfMonth)

// Intl resolves the locale from the browser; monthIndex is 0-based in the JS Date constructor.
@Suppress("UNUSED_PARAMETER")
private fun toLocaleDateString(
    year: Int,
    monthIndex: Int,
    day: Int,
): String =
    js(
        "new Date(year, monthIndex, day).toLocaleDateString(undefined, " +
            "{ year: 'numeric', month: 'short', day: 'numeric' })",
    )

actual fun formatShortMonthYear(
    year: Int,
    month: Int,
): String = toLocaleMonthYearString(year, month - 1)

actual fun formatNarrowMonth(month: Int): String = toLocaleNarrowMonthString(month - 1)

@Suppress("UNUSED_PARAMETER")
private fun toLocaleMonthYearString(
    year: Int,
    monthIndex: Int,
): String = js("new Date(year, monthIndex, 1).toLocaleDateString(undefined, { year: 'numeric', month: 'short' })")

// Any year will do: only the month is shown.
@Suppress("UNUSED_PARAMETER")
private fun toLocaleNarrowMonthString(monthIndex: Int): String =
    js("new Date(2000, monthIndex, 1).toLocaleDateString(undefined, { month: 'narrow' })")
