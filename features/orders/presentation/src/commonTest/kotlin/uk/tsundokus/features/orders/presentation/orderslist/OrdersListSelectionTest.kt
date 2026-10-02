package uk.tsundokus.features.orders.presentation.orderslist

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.sync.LastServerContactStore
import uk.tsundokus.core.domain.sync.PendingWrites
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.preferences.OrderSortPreference
import uk.tsundokus.features.orders.domain.preferences.OrdersPreferences
import uk.tsundokus.features.orders.presentation.selection.RecordingOrderRepository
import uk.tsundokus.features.orders.presentation.selection.SelectionChange
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private val orders =
    listOf(
        Order(id = "a", title = "Berserk 41", status = OrderStatus.SHIPPED, createdAt = 3),
        Order(id = "b", title = "Berserk 42", status = OrderStatus.ORDERED, createdAt = 2),
        Order(id = "c", title = "Akira 1", status = OrderStatus.RECEIVED, createdAt = 1),
    )

private class FixedOrdersPreferences : OrdersPreferences {
    override fun sort(): Flow<OrderSortPreference> = flowOf(OrderSortPreference())

    override suspend fun setSort(preference: OrderSortPreference) = Unit
}

private class NothingPending : PendingWrites {
    override fun observeCount(): Flow<Int> = flowOf(0)
}

private class NeverSynced : LastServerContactStore {
    override val lastContactAt: StateFlow<Long?> = MutableStateFlow(null)

    override fun record(serverTimeMillis: Long) = Unit

    override fun clear() = Unit
}

class OrdersListSelectionTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private val repository = RecordingOrderRepository(orders)

    // Lazy: the ViewModel must start after setUp has swapped in the test Main dispatcher.
    private val sut by lazy {
        OrdersListViewModel(
            orderRepository = repository,
            ordersPreferences = FixedOrdersPreferences(),
            pendingWrites = NothingPending(),
            lastServerContactStore = NeverSynced(),
        )
    }

    @Test
    fun `the select button starts picking with nothing picked`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnStartPicking)

                assertEquals(emptySet(), expectMostRecentItem().picked)
            }
        }

    @Test
    fun `picking all takes only the orders the filter shows`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnStatusFilterSelected(OrderStatus.SHIPPED))
                expectMostRecentItem()

                sut.onAction(OrdersListAction.OnPickAllShown)

                assertEquals(setOf("a"), expectMostRecentItem().picked)
            }
        }

    @Test
    fun `an order deleted elsewhere drops out of the picked ones`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnPickAllShown)
                expectMostRecentItem()

                repository.stored.value = orders.filterNot { it.id == "b" }

                assertEquals(setOf("a", "c"), expectMostRecentItem().picked)
            }
        }

    @Test
    fun `a change offers an undo and the undo restores the orders`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnTogglePicked("a"))
                sut.onAction(OrdersListAction.OnTogglePicked("b"))
                expectMostRecentItem()

                sut.events.test {
                    sut.onAction(OrdersListAction.OnChangePicked(SelectionChange.Status(OrderStatus.RECEIVED)))
                    assertIs<OrdersListEvent.ShowUndoableMessage>(awaitItem())
                }
                val afterChange = expectMostRecentItem()
                assertNull(afterChange.picked)
                assertEquals(3, afterChange.allOrders.count { it.status == OrderStatus.RECEIVED })

                sut.onAction(OrdersListAction.OnUndo)

                assertEquals(orders, expectMostRecentItem().allOrders)
            }
        }

    @Test
    fun `a change none of the picked orders can take says so without an undo`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnTogglePicked("c"))
                expectMostRecentItem()

                sut.events.test {
                    sut.onAction(OrdersListAction.OnChangePicked(SelectionChange.Status(OrderStatus.RECEIVED)))
                    assertIs<OrdersListEvent.ShowMessage>(awaitItem())
                }
                // Nothing changed, so there is no new state to await.
                assertEquals(setOf("c"), sut.state.value.picked)
            }
        }

    @Test
    fun `deleting the picked orders removes them in one batch`() =
        runTest {
            sut.state.test {
                sut.onAction(OrdersListAction.OnTogglePicked("a"))
                sut.onAction(OrdersListAction.OnTogglePicked("c"))
                expectMostRecentItem()

                sut.onAction(OrdersListAction.OnDeletePicked)

                assertEquals(listOf("b"), expectMostRecentItem().allOrders.map { it.id })
                assertEquals(1, repository.deletedBatches.size)
            }
        }
}
