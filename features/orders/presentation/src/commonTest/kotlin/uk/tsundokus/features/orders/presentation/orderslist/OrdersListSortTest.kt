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
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.models.SortDirection
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.preferences.OrderSortPreference
import uk.tsundokus.features.orders.domain.preferences.OrdersPreferences
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val orders =
    listOf(
        Order(id = "a", title = "Berserk", releaseDate = "2026-05-01", price = 5.0, createdAt = 1),
        Order(id = "b", title = "akira", releaseDate = "", price = 30.0, createdAt = 3),
        Order(id = "c", title = "Chainsaw Man", releaseDate = "2026-01-01", price = 10.0, createdAt = 2),
    )

private class StubOrderRepository : OrderRepository {
    override fun getOrders(): Flow<List<Order>> = flowOf(orders)

    override fun getOrderById(id: String): Flow<Order?> = flowOf(null)

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = Result.Success(order)

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = Result.Success(order)

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> = Result.Success(Order(id = id, title = ""))

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> = Result.Success(Order(id = id, title = ""))

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> = Result.Success(Order(id = id, title = ""))
}

private class FakeOrdersPreferences(
    initial: OrderSortPreference = OrderSortPreference(),
) : OrdersPreferences {
    val stored = MutableStateFlow(initial)

    override fun sort(): Flow<OrderSortPreference> = stored

    override suspend fun setSort(preference: OrderSortPreference) {
        stored.value = preference
    }
}

private class NoPendingWrites : PendingWrites {
    override fun observeCount(): Flow<Int> = flowOf(0)
}

private class NoServerContact : LastServerContactStore {
    override val lastContactAt: StateFlow<Long?> = MutableStateFlow(null)

    override fun record(serverTimeMillis: Long) = Unit

    override fun clear() = Unit
}

class OrdersListSortTest {
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

    private fun viewModel(preferences: OrdersPreferences) =
        OrdersListViewModel(
            orderRepository = StubOrderRepository(),
            ordersPreferences = preferences,
            pendingWrites = NoPendingWrites(),
            lastServerContactStore = NoServerContact(),
        )

    private suspend fun idsFor(preference: OrderSortPreference): List<String> {
        val sut = viewModel(FakeOrdersPreferences(preference))
        lateinit var ids: List<String>
        sut.state.test {
            var latest = awaitItem()
            while (latest.isLoading) latest = awaitItem()
            ids = latest.displayed.map { it.id }
            cancelAndIgnoreRemainingEvents()
        }
        return ids
    }

    @Test
    fun `title sorts case-insensitively and reverses`() =
        runTest {
            assertEquals(
                listOf("b", "a", "c"),
                idsFor(OrderSortPreference(OrderSort.TITLE, SortDirection.ASCENDING)),
            )
            assertEquals(
                listOf("c", "a", "b"),
                idsFor(OrderSortPreference(OrderSort.TITLE, SortDirection.DESCENDING)),
            )
        }

    @Test
    fun `price reverses`() =
        runTest {
            assertEquals(
                listOf("b", "c", "a"),
                idsFor(OrderSortPreference(OrderSort.PRICE, SortDirection.DESCENDING)),
            )
            assertEquals(
                listOf("a", "c", "b"),
                idsFor(OrderSortPreference(OrderSort.PRICE, SortDirection.ASCENDING)),
            )
        }

    @Test
    fun `an unset release date stays last in both directions`() =
        runTest {
            // "b" has no release date. Reversing must not float it to the top: unknown is not
            // "latest", and a plain comparator reversal would put it first.
            assertEquals(
                listOf("c", "a", "b"),
                idsFor(OrderSortPreference(OrderSort.RELEASE, SortDirection.ASCENDING)),
            )
            assertEquals(
                listOf("a", "c", "b"),
                idsFor(OrderSortPreference(OrderSort.RELEASE, SortDirection.DESCENDING)),
            )
        }

    @Test
    fun `re-picking the active sort flips its direction`() =
        runTest {
            val preferences = FakeOrdersPreferences(OrderSortPreference(OrderSort.TITLE, SortDirection.ASCENDING))
            val sut = viewModel(preferences)

            sut.onAction(OrdersListAction.OnSortSelected(OrderSort.TITLE))

            assertEquals(
                OrderSortPreference(OrderSort.TITLE, SortDirection.DESCENDING),
                preferences.stored.value,
            )
        }

    @Test
    fun `picking a different sort adopts that sort's own default direction`() =
        runTest {
            val preferences = FakeOrdersPreferences(OrderSortPreference(OrderSort.TITLE, SortDirection.DESCENDING))
            val sut = viewModel(preferences)

            sut.onAction(OrdersListAction.OnSortSelected(OrderSort.PRICE))

            // PRICE defaults to descending (priciest first), not to the direction TITLE was left in.
            assertEquals(
                OrderSortPreference(OrderSort.PRICE, SortDirection.DESCENDING),
                preferences.stored.value,
            )
        }
}
