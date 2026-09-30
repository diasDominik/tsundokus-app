package uk.tsundokus.features.orders.presentation.reminders

import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeParseException

/**
 * The instant a reminder for [date] (ISO) at [hour]:[minute] fires in [zone], as epoch millis; null for
 * a date that doesn't parse. Daylight saving is resolved the way people expect: a time that doesn't
 * exist (clocks jump forward) moves on by the jump, and a time that happens twice (clocks go back)
 * fires the first time.
 */
internal fun reminderTriggerMillis(
    date: String,
    hour: Int,
    minute: Int,
    zone: ZoneId,
): Long? =
    try {
        LocalDate
            .parse(date)
            .atTime(hour, minute)
            .atZone(zone)
            .toInstant()
            .toEpochMilli()
    } catch (_: DateTimeParseException) {
        null
    }

/**
 * How long from now, on [clock], until that reminder fires in the clock's time zone; null when its
 * moment has already passed (or the date doesn't parse), so it isn't scheduled.
 */
internal fun reminderDelayMillis(
    date: String,
    hour: Int,
    minute: Int,
    clock: Clock,
): Long? =
    reminderTriggerMillis(date, hour, minute, clock.zone)
        ?.minus(clock.millis())
        ?.takeIf { it > 0 }
