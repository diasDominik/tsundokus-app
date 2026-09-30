package uk.tsundokus.features.orders.presentation.reminders

import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

private val BERLIN = ZoneId.of("Europe/Berlin")
private val NEW_YORK = ZoneId.of("America/New_York")

private fun instant(iso: String): Long = Instant.parse(iso).toEpochMilli()

private fun clockAt(
    iso: String,
    zone: ZoneId,
): Clock = Clock.fixed(Instant.parse(iso), zone)

class ReminderTriggerTest {
    @Test
    fun `a reminder fires at the chosen time in the device's zone`() {
        // October: Berlin is on summer time, UTC+2.
        assertEquals(instant("2026-10-05T07:00:00Z"), reminderTriggerMillis("2026-10-05", 9, 0, BERLIN))
    }

    @Test
    fun `the same date and time are different moments in different zones`() {
        assertEquals(
            instant("2026-10-05T00:00:00Z"),
            reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("Asia/Tokyo")),
        )
        assertEquals(
            instant("2026-10-05T16:00:00Z"),
            reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("America/Los_Angeles")),
        )
    }

    @Test
    fun `half and three-quarter hour zones are honoured`() {
        assertEquals(
            instant("2026-10-05T03:30:00Z"),
            reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("Asia/Kolkata")),
        )
        assertEquals(
            instant("2026-10-05T03:15:00Z"),
            reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("Asia/Kathmandu")),
        )
    }

    @Test
    fun `either side of the date line the same local morning is a day apart`() {
        val kiritimati = assertNotNull(reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("Pacific/Kiritimati")))
        val pagoPago = assertNotNull(reminderTriggerMillis("2026-10-05", 9, 0, ZoneId.of("Pacific/Pago_Pago")))

        // UTC+14 against UTC-11.
        assertEquals(TimeUnit.HOURS.toMillis(25), pagoPago - kiritimati)
    }

    @Test
    fun `after moving to another zone the reminder is recomputed for the new one`() {
        val now = "2026-10-01T12:00:00Z"
        val inBerlin = assertNotNull(reminderDelayMillis("2026-10-05", 9, 0, clockAt(now, BERLIN)))
        val inNewYork = assertNotNull(reminderDelayMillis("2026-10-05", 9, 0, clockAt(now, NEW_YORK)))

        // 9:00 in New York comes six hours after 9:00 in Berlin.
        assertEquals(TimeUnit.HOURS.toMillis(6), inNewYork - inBerlin)
    }

    @Test
    fun `a time the clocks skip moves on by the jump`() {
        // 2026-03-29: Berlin jumps from 02:00 to 03:00; 02:30 doesn't happen, so 03:30 summer time.
        assertEquals(instant("2026-03-29T01:30:00Z"), reminderTriggerMillis("2026-03-29", 2, 30, BERLIN))
    }

    @Test
    fun `a time the clocks repeat fires the first time`() {
        // 2026-10-25: Berlin goes back from 03:00 to 02:00; 02:30 happens twice, first in summer time.
        assertEquals(instant("2026-10-25T00:30:00Z"), reminderTriggerMillis("2026-10-25", 2, 30, BERLIN))
    }

    @Test
    fun `a delay spanning the change to winter time still lands at the chosen hour`() {
        // Scheduled in summer time for a morning after the clocks went back: 9:00 is then 08:00 UTC.
        val clock = clockAt("2026-10-20T07:00:00Z", BERLIN)

        val delay = assertNotNull(reminderDelayMillis("2026-10-27", 9, 0, clock))

        assertEquals(instant("2026-10-27T08:00:00Z"), clock.millis() + delay)
    }

    @Test
    fun `a moment that has passed is not scheduled`() {
        val clock = clockAt("2026-10-05T07:00:00Z", BERLIN)

        assertNull(reminderDelayMillis("2026-10-05", 9, 0, clock), "exactly now")
        assertNull(reminderDelayMillis("2026-10-05", 8, 59, clock), "a minute ago")
        assertEquals(TimeUnit.MINUTES.toMillis(1), reminderDelayMillis("2026-10-05", 9, 1, clock))
    }

    @Test
    fun `yesterday's reminder in UTC can still be ahead on the device`() {
        // 08:00 UTC on 1 October is 22:00 on 30 September in Hawaii (UTC-10): a reminder set for
        // 23:00 that day is still an hour away, though its date is already yesterday in UTC.
        val clock = clockAt("2026-10-01T08:00:00Z", ZoneId.of("Pacific/Honolulu"))

        assertEquals(TimeUnit.HOURS.toMillis(1), reminderDelayMillis("2026-09-30", 23, 0, clock))
    }

    @Test
    fun `a reminder on the 29th of February fires that morning`() {
        // February: Berlin is on winter time, UTC+1.
        assertEquals(instant("2028-02-29T08:00:00Z"), reminderTriggerMillis("2028-02-29", 9, 0, BERLIN))
    }

    @Test
    fun `a delay over the 29th of February counts the extra day`() {
        val clock = clockAt("2028-02-28T08:00:00Z", BERLIN)

        assertEquals(TimeUnit.DAYS.toMillis(2), reminderDelayMillis("2028-03-01", 9, 0, clock))
    }

    @Test
    fun `the 29th of February in a year without one is not scheduled`() {
        assertNull(reminderTriggerMillis("2027-02-29", 9, 0, BERLIN))
    }

    @Test
    fun `a date that doesn't exist is not scheduled`() {
        assertNull(reminderTriggerMillis("2026-02-30", 9, 0, BERLIN))
        assertNull(reminderTriggerMillis("soon", 9, 0, BERLIN))
    }
}
