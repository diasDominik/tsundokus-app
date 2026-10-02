package uk.tsundokus.features.orders.presentation.scantoreceive

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ISBN = "9784088820453"

/** Keeps orders in memory and records every update, the way the offline-first repository would. */
private class InMemoryOrderRepository(
    orders: List<Order>,
) : OrderRepository {
    val orders = MutableStateFlow(orders.associateBy(Order::id))
    val updates = mutableListOf<Order>()
    var failUpdates = false

    override fun getOrders(): Flow<List<Order>> = orders.map { it.values.toList() }

    override fun getOrderById(id: String): Flow<Order?> = orders.map { it[id] }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = updateOrder(order)

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> {
        if (failUpdates) return Result.Failure(DataError.Remote.NO_INTERNET)
        updates += order
        orders.value += order.id to order
        return Result.Success(order)
    }

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun updateOrders(orders: List<Order>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun deleteOrders(ids: Collection<String>): EmptyResult<DataError.Remote> = error("unused")

    override suspend fun setStatus(
        id: String,
        status: OrderStatus,
    ): Result<Order, DataError.Remote> = error("not used by the scanner")

    override suspend fun reportDelay(
        id: String,
        delayedTo: String,
    ): Result<Order, DataError.Remote> = error("not used by the scanner")

    override suspend fun setReadState(
        id: String,
        readState: ReadState,
    ): Result<Order, DataError.Remote> = error("not used by the scanner")
}

class ScanToReceiveViewModelTest {
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

    private val shipped =
        Order(id = "1", title = "Chainsaw Man", isbn = ISBN, status = OrderStatus.SHIPPED, shipDate = "2026-09-01")
    private val noIsbn = Order(id = "2", title = "Berserk", status = OrderStatus.ORDERED)

    @Test
    fun `scanning an open order marks it received with a date`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped))
            val sut = ScanToReceiveViewModel(repository)

            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            val saved = repository.updates.single()
            assertEquals(OrderStatus.RECEIVED, saved.status)
            assertTrue(saved.receivedDate.isNotBlank())
            assertEquals(ScanOutcome.Received(saved, previous = shipped), sut.state.value.outcome)
        }

    @Test
    fun `the same book read twice in a row is handled once`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped))
            val sut = ScanToReceiveViewModel(repository)

            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))
            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            assertEquals(1, repository.updates.size)
            assertIs<ScanOutcome.Received>(sut.state.value.outcome)
        }

    @Test
    fun `undo puts the order back as it was`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped))
            val sut = ScanToReceiveViewModel(repository)
            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            sut.onAction(ScanToReceiveAction.OnUndo)

            assertEquals(shipped, repository.orders.value.getValue("1"))
            assertEquals(ScanOutcome.Undone(shipped), sut.state.value.outcome)
            // Cleared so the same book can be scanned again right away.
            assertNull(sut.state.value.lastIsbn)
        }

    @Test
    fun `an unknown ISBN asks which order it belongs to`() =
        runTest {
            val sut = ScanToReceiveViewModel(InMemoryOrderRepository(listOf(noIsbn)))

            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            assertEquals(OrderPick(ISBN, OrderPick.Reason.UNKNOWN, listOf(noIsbn)), sut.state.value.picking)
            assertTrue(!sut.state.value.isCameraActive)
        }

    @Test
    fun `picking an order gives it the ISBN and receives it`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(noIsbn))
            val sut = ScanToReceiveViewModel(repository)
            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            sut.onAction(ScanToReceiveAction.OnOrderPicked("2"))

            val saved = repository.orders.value.getValue("2")
            assertEquals(ISBN, saved.isbn)
            assertEquals(OrderStatus.RECEIVED, saved.status)
            assertNull(sut.state.value.picking)
        }

    @Test
    fun `undoing a pick removes the ISBN it attached`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(noIsbn))
            val sut = ScanToReceiveViewModel(repository)
            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))
            sut.onAction(ScanToReceiveAction.OnOrderPicked("2"))

            sut.onAction(ScanToReceiveAction.OnUndo)

            assertEquals(
                "",
                repository.orders.value
                    .getValue("2")
                    .isbn,
            )
        }

    @Test
    fun `adding an unknown ISBN as a new order hands the ISBN on`() =
        runTest {
            val sut = ScanToReceiveViewModel(InMemoryOrderRepository(emptyList()))
            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            sut.events.test {
                sut.onAction(ScanToReceiveAction.OnAddAsNewOrder)
                assertEquals(ScanToReceiveEvent.AddOrder(ISBN), awaitItem())
            }
            assertNull(sut.state.value.picking)
        }

    @Test
    fun `a received book reports when it arrived`() =
        runTest {
            val received = shipped.copy(status = OrderStatus.RECEIVED, receivedDate = "2026-09-20")
            val repository = InMemoryOrderRepository(listOf(received))
            val sut = ScanToReceiveViewModel(repository)

            sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))

            assertEquals(ScanOutcome.AlreadyReceived(received), sut.state.value.outcome)
            assertTrue(repository.updates.isEmpty())
        }

    @Test
    fun `thirteen typed digits are looked up without pressing Enter`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped))
            val sut = ScanToReceiveViewModel(repository)

            sut.onAction(ScanToReceiveAction.OnManualIsbnChange("978-4-08-882045-3"))

            assertEquals(1, repository.updates.size)
            assertEquals("", sut.state.value.manualIsbn)
        }

    @Test
    fun `a typed ISBN-10 waits for submit`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped))
            val sut = ScanToReceiveViewModel(repository)

            sut.onAction(ScanToReceiveAction.OnManualIsbnChange("4088820452"))
            assertTrue(repository.updates.isEmpty())

            sut.onAction(ScanToReceiveAction.OnManualIsbnSubmit)
            assertEquals(1, repository.updates.size)
        }

    @Test
    fun `submitting an invalid ISBN flags the field`() =
        runTest {
            val sut = ScanToReceiveViewModel(InMemoryOrderRepository(listOf(shipped)))

            sut.onAction(ScanToReceiveAction.OnManualIsbnChange("12345"))
            sut.onAction(ScanToReceiveAction.OnManualIsbnSubmit)

            assertTrue(sut.state.value.isManualIsbnInvalid)
        }

    @Test
    fun `a failed save reports the error and leaves the order alone`() =
        runTest {
            val repository = InMemoryOrderRepository(listOf(shipped)).apply { failUpdates = true }
            val sut = ScanToReceiveViewModel(repository)

            sut.events.test {
                sut.onAction(ScanToReceiveAction.OnIsbnScanned(ISBN))
                assertIs<ScanToReceiveEvent.ShowError>(awaitItem())
            }
            assertEquals(shipped, repository.orders.value.getValue("1"))
            assertTrue(sut.state.value.isCameraActive)
        }
}
