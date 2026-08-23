package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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
import kotlin.test.assertTrue

private class RecordingOrderRepository : OrderRepository {
    var created: Order? = null

    override fun getOrders(): Flow<List<Order>> = flowOf(emptyList())

    override fun getOrderById(id: String): Flow<Order?> = flowOf(null)

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> {
        created = order
        return Result.Success(order)
    }

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

class AddEditOrderValidationTest {
    // viewModelScope dispatches on Main, so saves only run once Main is a test dispatcher.
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

    private fun viewModel(repository: OrderRepository = RecordingOrderRepository()) =
        AddEditOrderViewModel(orderId = null, orderRepository = repository)

    private fun AddEditOrderViewModel.fillValidForm() {
        onAction(AddEditOrderAction.OnTitleChange("Berserk"))
        onAction(AddEditOrderAction.OnAuthorChange("Kentaro Miura"))
        onAction(AddEditOrderAction.OnPublisherChange("Dark Horse"))
        onAction(AddEditOrderAction.OnStoreChange("Amazon"))
        onAction(AddEditOrderAction.OnPriceChange("19.99"))
        onAction(AddEditOrderAction.OnOrderDateChange("2026-03-07"))
    }

    @Test
    fun `an empty form reports every required field at once`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnSave)
            assertEquals(OrderFormField.entries.toSet(), sut.state.value.errors)
        }

    @Test
    fun `volume and release date are not required`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnSave)

            assertTrue(
                sut.state.value.errors
                    .isEmpty(),
            )
            assertEquals("", repository.created?.volume)
            assertEquals("", repository.created?.releaseDate)
        }

    @Test
    fun `a blank required field blocks the save`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnStoreChange("   "))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals(setOf(OrderFormField.STORE), sut.state.value.errors)
            assertEquals(null, repository.created)
        }

    @Test
    fun `editing a field clears only its own error`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnSave)
            sut.onAction(AddEditOrderAction.OnTitleChange("Berserk"))

            val errors = sut.state.value.errors
            assertTrue(OrderFormField.TITLE !in errors)
            assertTrue(OrderFormField.AUTHOR in errors)
        }

    @Test
    fun `a price of letters never reaches the state so it fails as blank`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnPriceChange("abc"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals("", sut.state.value.price)
            assertEquals(setOf(OrderFormField.PRICE), sut.state.value.errors)
        }

    @Test
    fun `a zero price is accepted`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnPriceChange("0"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertTrue(
                sut.state.value.errors
                    .isEmpty(),
            )
        }
}
