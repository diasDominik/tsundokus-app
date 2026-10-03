package uk.tsundokus.features.orders.presentation.widgets

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.auth.UserType
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.selection.RecordingOrderRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private class RecordingPublisher : WidgetPublisher {
    val published = mutableListOf<WidgetSnapshot>()

    override suspend fun publish(snapshot: WidgetSnapshot) {
        published += snapshot
    }
}

private class SwitchableSession(
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

// Far enough ahead that "today" never catches up with it while the suite exists.
private val shipped = Order(id = "1", title = "One Piece", status = OrderStatus.SHIPPED, eta = "2099-01-10")

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSyncTest {
    @Test
    fun `the widgets get the orders as soon as it starts`() =
        runTest(UnconfinedTestDispatcher()) {
            val publisher = RecordingPublisher()
            WidgetSync(
                RecordingOrderRepository(listOf(shipped)),
                SwitchableSession(true),
                publisher,
                backgroundScope,
            ).start()

            assertEquals(
                listOf("1"),
                publisher.published
                    .single()
                    .arrivals
                    .map { it.id },
            )
        }

    @Test
    fun `a change the widgets don't show redraws nothing`() =
        runTest(UnconfinedTestDispatcher()) {
            val publisher = RecordingPublisher()
            val orders = RecordingOrderRepository(listOf(shipped))
            WidgetSync(orders, SwitchableSession(true), publisher, backgroundScope).start()

            // A price is a real change to the order, but not to anything a widget shows.
            orders.stored.value = listOf(shipped.copy(price = 9.99))

            assertEquals(1, publisher.published.size)
        }

    @Test
    fun `a change the widgets show redraws them`() =
        runTest(UnconfinedTestDispatcher()) {
            val publisher = RecordingPublisher()
            val orders = RecordingOrderRepository(listOf(shipped))
            WidgetSync(orders, SwitchableSession(true), publisher, backgroundScope).start()

            orders.stored.value = listOf(shipped.copy(status = OrderStatus.RECEIVED, readState = ReadState.WANT))

            assertEquals(2, publisher.published.size)
            assertEquals(1, publisher.published.last().unread)
        }

    @Test
    fun `signing out clears the widgets`() =
        runTest(UnconfinedTestDispatcher()) {
            val publisher = RecordingPublisher()
            val session = SwitchableSession(true)
            WidgetSync(RecordingOrderRepository(listOf(shipped)), session, publisher, backgroundScope).start()

            session.set(null)

            assertTrue(publisher.published.first().signedIn)
            assertEquals(WidgetSnapshot.SignedOut, publisher.published.last())
            assertFalse(
                publisher.published
                    .last()
                    .arrivals
                    .any(),
            )
        }

    @Test
    fun `starting twice follows the orders once`() =
        runTest(UnconfinedTestDispatcher()) {
            val publisher = RecordingPublisher()
            val sync =
                WidgetSync(
                    RecordingOrderRepository(listOf(shipped)),
                    SwitchableSession(true),
                    publisher,
                    backgroundScope,
                )

            sync.start()
            sync.start()

            assertEquals(1, publisher.published.size)
        }
}
