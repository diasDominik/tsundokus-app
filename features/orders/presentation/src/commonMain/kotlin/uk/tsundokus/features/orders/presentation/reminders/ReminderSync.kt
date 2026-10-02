package uk.tsundokus.features.orders.presentation.reminders

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.di.APPLICATION_SCOPE
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.preferences.ReminderPreferences
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.order.OrderRepository

/**
 * Keeps the scheduled reminders in step with the orders and the reminder settings. Everything is
 * worked out on the device from the cached orders; the system delivers them, so nothing runs in
 * between. Signed out, or with reminders off, the set is empty — which cancels what was scheduled.
 */
@Single
class ReminderSync(
    private val orderRepository: OrderRepository,
    private val reminderPreferences: ReminderPreferences,
    private val sessionStorage: SessionStorage,
    private val scheduler: ReminderScheduler,
    private val texts: ReminderTexts,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private data class Schedule(
        val reminders: List<PlannedReminder>,
        val hour: Int,
        val minute: Int,
    )

    private var job: Job? = null

    /** Idempotent: starts following orders and settings once, for the life of the app. */
    fun start() {
        if (job != null) return
        job =
            appScope.launch {
                combine(
                    orderRepository.getOrders(),
                    reminderPreferences.settings(),
                    sessionStorage.authState.map { it != null },
                ) { orders, settings, signedIn ->
                    val planned = if (signedIn) plannedReminders(orders, settings, todayIso()) else emptyList()
                    Schedule(planned, settings.hour, settings.minute)
                }
                    // A sync that changes nothing a reminder depends on schedules nothing.
                    .distinctUntilChanged()
                    .collectLatest { schedule ->
                        scheduler.replaceAll(
                            schedule.reminders.map { texts.reminderFor(it) },
                            schedule.hour,
                            schedule.minute,
                        )
                    }
            }
    }
}
