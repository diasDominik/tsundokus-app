package uk.tsundokus.features.orders.presentation.reminders

import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val TODAY = "2026-10-01"
private val ON = ReminderSettings(enabled = true)

private fun order(
    id: String,
    status: OrderStatus,
    eta: String = "",
    delayedTo: String = "",
) = Order(id = id, title = "One Piece", volume = "Vol. $id", status = status, eta = eta, delayedTo = delayedTo)

class PlannedRemindersTest {
    @Test
    fun `a shipped order is overdue three days after its ETA`() {
        val planned = plannedReminders(listOf(order("107", OrderStatus.SHIPPED, eta = "2026-10-02")), ON, TODAY)

        assertEquals(
            listOf(
                PlannedReminder(
                    "overdue-107",
                    "107",
                    ReminderKind.OVERDUE,
                    "2026-10-05",
                    "One Piece Vol. 107",
                    "2026-10-02",
                ),
            ),
            planned,
        )
    }

    @Test
    fun `a delayed order is reminded on its new date and overdue three days after it`() {
        val planned =
            plannedReminders(
                listOf(order("42", OrderStatus.DELAYED, eta = "2026-09-01", delayedTo = "2026-10-10")),
                ON,
                TODAY,
            )

        assertEquals(
            listOf("delayed-42" to "2026-10-10", "overdue-42" to "2026-10-13"),
            planned.map {
                it.id to
                    it.date
            },
        )
    }

    @Test
    fun `received and cancelled orders get no reminders`() {
        val orders =
            listOf(
                order("1", OrderStatus.RECEIVED, eta = "2026-10-02"),
                order("2", OrderStatus.CANCELLED, eta = "2026-10-02"),
            )

        assertTrue(plannedReminders(orders, ON, TODAY).isEmpty())
    }

    @Test
    fun `an order without an expected date gets no reminder`() {
        assertTrue(plannedReminders(listOf(order("1", OrderStatus.ORDERED)), ON, TODAY).isEmpty())
    }

    @Test
    fun `reminders whose day has passed are not planned`() {
        val planned = plannedReminders(listOf(order("1", OrderStatus.SHIPPED, eta = "2026-09-20")), ON, TODAY)

        assertTrue(planned.isEmpty())
    }

    @Test
    fun `a reminder due today is still planned`() {
        val planned = plannedReminders(listOf(order("1", OrderStatus.SHIPPED, eta = "2026-09-28")), ON, TODAY)

        assertEquals(listOf(TODAY), planned.map { it.date })
    }

    @Test
    fun `yesterday in UTC is kept since it can still be today on the device`() {
        // A late reminder time can still be ahead: 23:00 on 2026-09-30 in Hawaii is 09:00 UTC on 2026-10-01.
        // The device skips it if its moment has passed.
        val planned = plannedReminders(listOf(order("1", OrderStatus.SHIPPED, eta = "2026-09-27")), ON, TODAY)

        assertEquals(listOf("2026-09-30"), planned.map { it.date })
    }

    @Test
    fun `two days ago is past in every time zone`() {
        val planned = plannedReminders(listOf(order("1", OrderStatus.SHIPPED, eta = "2026-09-26")), ON, TODAY)

        assertTrue(planned.isEmpty())
    }

    @Test
    fun `an overdue reminder can fall in the next year`() {
        val planned = plannedReminders(listOf(order("1", OrderStatus.SHIPPED, eta = "2026-12-30")), ON, TODAY)

        assertEquals(listOf("2027-01-02"), planned.map { it.date })
    }

    @Test
    fun `a delayed order without a new date is overdue from its ETA`() {
        val planned = plannedReminders(listOf(order("1", OrderStatus.DELAYED, eta = "2026-10-02")), ON, TODAY)

        assertEquals(listOf("overdue-1" to "2026-10-05"), planned.map { it.id to it.date })
    }

    @Test
    fun `ids stay the same across plans so rescheduling replaces`() {
        val orders = listOf(order("1", OrderStatus.SHIPPED, eta = "2026-10-02"))

        assertEquals(plannedReminders(orders, ON, TODAY), plannedReminders(orders, ON, "2026-10-02"))
    }

    @Test
    fun `a delayed order due on the 29th of February is reminded that day and overdue in March`() {
        val planned =
            plannedReminders(listOf(order("7", OrderStatus.DELAYED, delayedTo = "2028-02-29")), ON, TODAY)

        assertEquals(
            listOf("delayed-7" to "2028-02-29", "overdue-7" to "2028-03-03"),
            planned.map {
                it.id to
                    it.date
            },
        )
    }

    @Test
    fun `an ETA three days before the 29th of February is overdue on it`() {
        val planned = plannedReminders(listOf(order("7", OrderStatus.SHIPPED, eta = "2028-02-26")), ON, TODAY)

        assertEquals(listOf("2028-02-29"), planned.map { it.date })
    }

    @Test
    fun `a 29th of February that doesn't exist gets no reminder`() {
        val orders =
            listOf(
                order("1", OrderStatus.SHIPPED, eta = "2027-02-29"),
                order("2", OrderStatus.DELAYED, delayedTo = "2027-02-29"),
            )

        assertTrue(plannedReminders(orders, ON, TODAY).isEmpty())
    }

    @Test
    fun `each kind can be switched off`() {
        val delayed = listOf(order("42", OrderStatus.DELAYED, delayedTo = "2026-10-10"))

        assertEquals(
            listOf("delayed-42"),
            plannedReminders(delayed, ON.copy(overdue = false), TODAY).map { it.id },
        )
        assertEquals(
            listOf(
                "overdue-42",
            ),
            plannedReminders(delayed, ON.copy(delayedDateReached = false), TODAY).map {
                it.id
            },
        )
    }

    @Test
    fun `nothing is planned while reminders are off`() {
        val orders = listOf(order("1", OrderStatus.SHIPPED, eta = "2026-10-02"))

        assertTrue(plannedReminders(orders, ReminderSettings(enabled = false), TODAY).isEmpty())
    }

    @Test
    fun `the soonest reminders come first and only fifty are kept`() {
        val orders =
            (1..80).map { day ->
                order("$day", OrderStatus.SHIPPED, eta = "2026-12-${(day % 28 + 1).toString().padStart(2, '0')}")
            }

        val planned = plannedReminders(orders, ON, TODAY)

        assertEquals(MAX_REMINDERS, planned.size)
        assertEquals(planned.sortedBy { it.date }, planned)
    }
}
