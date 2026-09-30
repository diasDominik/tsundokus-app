package uk.tsundokus.features.orders.presentation.reminders

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import java.util.TimeZone
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private fun reminder(
    id: String,
    date: String = "2099-01-13",
) = Reminder(id, date, "One Piece", "Still waiting?", "tsundokus://tsundokus.uk/orders/1")

@RunWith(AndroidJUnit4::class)
class WorkManagerReminderSchedulerTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val defaultZone = TimeZone.getDefault()
    private lateinit var sut: WorkManagerReminderScheduler

    @BeforeTest
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        sut = WorkManagerReminderScheduler(context)
    }

    @AfterTest
    fun tearDown() {
        TimeZone.setDefault(defaultZone)
    }

    private fun waitingReminders(): List<WorkInfo> =
        WorkManager
            .getInstance(context)
            .getWorkInfosByTag("reminders")
            .get()
            .filter { it.state == WorkInfo.State.ENQUEUED }

    private fun stateOf(reminderId: String): WorkInfo.State? =
        WorkManager
            .getInstance(context)
            .getWorkInfosForUniqueWork(reminderId)
            .get()
            .singleOrNull()
            ?.state

    @Test
    fun `each reminder waits as its own job`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1"), reminder("delayed-2")), 9, 0)

            assertEquals(2, waitingReminders().size)
        }

    @Test
    fun `switching off cancels every waiting reminder`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1"), reminder("delayed-2")), 9, 0)

            sut.replaceAll(emptyList(), 9, 0)

            assertTrue(waitingReminders().isEmpty())
        }

    @Test
    fun `a time zone change after switching off brings nothing back`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1")), 9, 0)
            sut.replaceAll(emptyList(), 9, 0)

            sut.rescheduleRemembered()

            assertTrue(waitingReminders().isEmpty())
        }

    @Test
    fun `switching back on schedules them again`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1")), 9, 0)
            sut.replaceAll(emptyList(), 9, 0)

            sut.replaceAll(listOf(reminder("overdue-1")), 9, 0)

            assertEquals(1, waitingReminders().size)
        }

    @Test
    fun `a reminder left out of the new set is cancelled and the rest stay`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1"), reminder("delayed-2")), 9, 0)

            sut.replaceAll(listOf(reminder("delayed-2")), 9, 0)

            assertEquals(WorkInfo.State.CANCELLED, stateOf("overdue-1"))
            assertEquals(WorkInfo.State.ENQUEUED, stateOf("delayed-2"))
        }

    @Test
    fun `a reminder whose moment has passed is not scheduled`() =
        runTest {
            sut.replaceAll(listOf(reminder("overdue-1", date = "2000-01-01")), 9, 0)

            assertTrue(waitingReminders().isEmpty())
        }

    @Test
    fun `a new time zone moves the waiting reminders to its 9 o'clock`() =
        runTest {
            TimeZone.setDefault(TimeZone.getTimeZone("Europe/Berlin"))
            sut.replaceAll(listOf(reminder("overdue-1")), 9, 0)
            val berlinDelay = waitingReminders().single().initialDelayMillis

            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"))
            sut.rescheduleRemembered()
            val tokyoDelay = waitingReminders().single().initialDelayMillis

            // In January Tokyo (UTC+9) reaches 9:00 eight hours before Berlin (UTC+1).
            val difference = berlinDelay - tokyoDelay
            assertTrue(abs(difference - TimeUnit.HOURS.toMillis(8)) < TimeUnit.MINUTES.toMillis(1))
        }
}
