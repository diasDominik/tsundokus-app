package uk.tsundokus.features.orders.presentation.reminders

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.auth.UserType
import uk.tsundokus.core.domain.preferences.ReminderPreferences
import uk.tsundokus.core.domain.preferences.ReminderSettings
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.presentation.navigation.orderDeepLink
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class RecordingScheduler : ReminderScheduler {
    val calls = mutableListOf<Triple<List<Reminder>, Int, Int>>()

    override suspend fun replaceAll(
        reminders: List<Reminder>,
        hour: Int,
        minute: Int,
    ) {
        calls += Triple(reminders, hour, minute)
    }
}

private class OrdersInMemory(
    orders: List<Order>,
) : OrderRepository {
    val orders = MutableStateFlow(orders)

    override fun getOrders(): Flow<List<Order>> = orders

    override fun getOrderById(id: String): Flow<Order?> = orders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun updateOrders(orders: List<Order>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun deleteOrders(ids: Collection<String>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> = error("unused")

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> = error("unused")

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> = error("unused")
}

private class StoredReminderSettings(
    initial: ReminderSettings,
) : ReminderPreferences {
    val settings = MutableStateFlow(initial)

    override fun settings(): Flow<ReminderSettings> = settings

    override suspend fun update(settings: ReminderSettings) {
        this.settings.value = settings
    }
}

private class Session(
    signedIn: Boolean,
) : SessionStorage {
    private val state = MutableStateFlow(if (signedIn) SIGNED_IN else null)
    override val authState: StateFlow<AuthInfo?> = state

    override fun get(): AuthInfo? = state.value

    override fun set(info: AuthInfo?) {
        state.value = info
    }

    override suspend fun load(): AuthInfo? = state.value
}

private val SIGNED_IN =
    AuthInfo(
        accessToken = "access",
        refreshToken = "refresh",
        user =
            User(
                id = "u1",
                email = "reader@example.com",
                username = "reader",
                hasVerifiedEmail = true,
                userType = UserType.REGISTERED,
            ),
    )

/** Plain words, so the test doesn't wait on string resources loading from disk. */
private val plainTexts =
    ReminderTexts { planned ->
        Reminder(planned.id, planned.date, planned.orderLabel, planned.kind.name, orderDeepLink(planned.orderId))
    }

// Far enough ahead that "today" never catches up with it while the suite exists.
private val shipped = Order(id = "1", title = "One Piece", status = OrderStatus.SHIPPED, eta = "2099-01-10")

private val delayed =
    Order(id = "2", title = "Berserk", status = OrderStatus.DELAYED, eta = "2099-01-10", delayedTo = "2099-02-01")

@OptIn(ExperimentalCoroutinesApi::class)
class ReminderSyncTest {
    @Test
    fun `reminders are scheduled at the chosen time`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                StoredReminderSettings(ReminderSettings(enabled = true, hour = 8, minute = 30)),
                Session(signedIn = true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            val (reminders, hour, minute) = scheduler.calls.last()
            assertEquals(listOf("overdue-1"), reminders.map { it.id })
            assertEquals(8 to 30, hour to minute)
            assertEquals("2099-01-13", reminders.single().date)
        }

    @Test
    fun `a change that alters no reminder schedules nothing new`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val orders = OrdersInMemory(listOf(shipped))
            ReminderSync(
                orders,
                StoredReminderSettings(ReminderSettings(enabled = true)),
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            // A new read state is a real change to the orders, but not to any reminder.
            orders.orders.value = listOf(shipped.copy(readState = ReadState.READ))

            assertEquals(1, scheduler.calls.size)
        }

    @Test
    fun `switching reminders off cancels them`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val settings = StoredReminderSettings(ReminderSettings(enabled = true))
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                settings,
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            settings.update(ReminderSettings(enabled = false))

            assertTrue(
                scheduler.calls
                    .last()
                    .first
                    .isEmpty(),
            )
        }

    @Test
    fun `switching reminders back on schedules them again`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val settings = StoredReminderSettings(ReminderSettings(enabled = true))
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                settings,
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            settings.update(ReminderSettings(enabled = false))
            settings.update(ReminderSettings(enabled = true))

            assertEquals(
                listOf(listOf("overdue-1"), emptyList(), listOf("overdue-1")),
                scheduler.calls.map { (reminders) -> reminders.map { it.id } },
            )
        }

    @Test
    fun `starting with reminders off clears any left from before`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                StoredReminderSettings(ReminderSettings(enabled = false)),
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            assertEquals(1, scheduler.calls.size)
            assertTrue(
                scheduler.calls
                    .single()
                    .first
                    .isEmpty(),
            )
        }

    @Test
    fun `turning one kind off cancels only that kind`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val settings = StoredReminderSettings(ReminderSettings(enabled = true))
            ReminderSync(
                OrdersInMemory(listOf(delayed)),
                settings,
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()
            assertEquals(
                listOf("delayed-2", "overdue-2"),
                scheduler.calls
                    .last()
                    .first
                    .map { it.id },
            )

            settings.update(ReminderSettings(enabled = true, overdue = false))
            assertEquals(
                listOf("delayed-2"),
                scheduler.calls
                    .last()
                    .first
                    .map { it.id },
            )

            settings.update(ReminderSettings(enabled = true, overdue = true, delayedDateReached = false))
            assertEquals(
                listOf("overdue-2"),
                scheduler.calls
                    .last()
                    .first
                    .map { it.id },
            )
        }

    @Test
    fun `turning both kinds off cancels all of them`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val settings = StoredReminderSettings(ReminderSettings(enabled = true))
            ReminderSync(
                OrdersInMemory(listOf(shipped, delayed)),
                settings,
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            settings.update(ReminderSettings(enabled = true, overdue = false, delayedDateReached = false))

            assertTrue(
                scheduler.calls
                    .last()
                    .first
                    .isEmpty(),
            )
        }

    @Test
    fun `a new time moves the same reminders to it`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val settings = StoredReminderSettings(ReminderSettings(enabled = true))
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                settings,
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            settings.update(ReminderSettings(enabled = true, hour = 20, minute = 15))

            val (before, after) = scheduler.calls
            assertEquals(before.first, after.first)
            assertEquals(20 to 15, after.second to after.third)
        }

    @Test
    fun `a received order loses its reminder`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val orders = OrdersInMemory(listOf(shipped, delayed))
            ReminderSync(
                orders,
                StoredReminderSettings(ReminderSettings(enabled = true)),
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            orders.orders.value = listOf(shipped.copy(status = OrderStatus.RECEIVED), delayed)

            assertEquals(
                listOf("delayed-2", "overdue-2"),
                scheduler.calls
                    .last()
                    .first
                    .map { it.id },
            )
        }

    @Test
    fun `signing back in schedules them again`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val session = Session(signedIn = true)
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                StoredReminderSettings(ReminderSettings(enabled = true)),
                session,
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            session.set(null)
            session.set(SIGNED_IN)

            assertEquals(
                listOf("overdue-1"),
                scheduler.calls
                    .last()
                    .first
                    .map { it.id },
            )
        }

    @Test
    fun `signing out cancels them`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            val session = Session(signedIn = true)
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                StoredReminderSettings(ReminderSettings(enabled = true)),
                session,
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            session.set(null)

            assertTrue(
                scheduler.calls
                    .last()
                    .first
                    .isEmpty(),
            )
        }

    @Test
    fun `a tapped reminder opens its order`() =
        runTest(UnconfinedTestDispatcher()) {
            val scheduler = RecordingScheduler()
            ReminderSync(
                OrdersInMemory(listOf(shipped)),
                StoredReminderSettings(ReminderSettings(enabled = true)),
                Session(true),
                scheduler,
                plainTexts,
                backgroundScope,
            ).start()

            assertEquals(
                "tsundokus://tsundokus.uk/orders/1",
                scheduler.calls
                    .last()
                    .first
                    .single()
                    .deepLink,
            )
        }
}
