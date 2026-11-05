package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private const val ISBN = "9784088820453"
private const val ISBN_10_OF_ISBN = "4088820452"
private const val OTHER_ISBN = "9780439420891"

private fun order(
    id: String,
    isbn: String = ISBN,
    status: OrderStatus = OrderStatus.ORDERED,
) = Order(
    id = id,
    title = "t",
    author = "a",
    publisher = "p",
    store = "s",
    price = 1.0,
    orderDate = "2026-09-01",
    isbn = isbn,
    status = status,
)

/** Echoes every write back through [getOrders], as the Room cache does. */
private class CachingOrderRepository(
    vararg orders: Order,
) : OrderRepository {
    val orders = MutableStateFlow(orders.toList())
    var writes = 0

    override fun getOrders(): Flow<List<Order>> = orders

    override fun getOrderById(id: String): Flow<Order?> = orders.map { all -> all.find { it.id == id } }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> {
        writes++
        orders.value = orders.value.filterNot { it.id == order.id } + order
        return Result.Success(order)
    }

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = createOrder(order)

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

private object DuplicateTestPreferences : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override fun currency(): Flow<AppCurrency> = flowOf(AppCurrency.EUR)

    override suspend fun setCurrency(currency: AppCurrency) = Unit
}

class AddEditOrderDuplicateIsbnTest {
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

    private fun viewModel(
        repository: OrderRepository,
        args: AddEditOrderArgs = AddEditOrderArgs(),
    ) = AddEditOrderViewModel(
        args = args,
        orderRepository = repository,
        appPreferencesRepository = DuplicateTestPreferences,
        bookRepository = FakeBookRepository(),
    )

    private fun AddEditOrderViewModel.fillRequiredFields() {
        onAction(AddEditOrderAction.OnTitleChange("t"))
        onAction(AddEditOrderAction.OnAuthorChange("a"))
        onAction(AddEditOrderAction.OnPublisherChange("p"))
        onAction(AddEditOrderAction.OnStoreChange("s"))
        onAction(AddEditOrderAction.OnPriceChange("1"))
        onAction(AddEditOrderAction.OnOrderDateChange("2026-09-01"))
    }

    private val AddEditOrderViewModel.isbnFlagged: Boolean
        get() = OrderFormField.ISBN in state.value.errors

    @Test
    fun `a new order with a taken ISBN is not saved`() {
        val repository = CachingOrderRepository(order("1"))
        val sut = viewModel(repository)
        sut.fillRequiredFields()
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        sut.onAction(AddEditOrderAction.OnSave)

        assertTrue(sut.isbnFlagged)
        assertEquals(0, repository.writes)
    }

    @Test
    fun `a taken ISBN is flagged as soon as it is scanned`() {
        val sut = viewModel(CachingOrderRepository(order("1")))

        sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))

        assertTrue(sut.isbnFlagged)
    }

    @Test
    fun `the old ISBN-10 of a taken ISBN is taken too`() {
        val sut = viewModel(CachingOrderRepository(order("1")))

        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN_10_OF_ISBN))

        assertTrue(sut.isbnFlagged)
    }

    @Test
    fun `changing to a free ISBN clears the flag and saves`() {
        val repository = CachingOrderRepository(order("1"))
        val sut = viewModel(repository)
        sut.fillRequiredFields()
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        sut.onAction(AddEditOrderAction.OnIsbnChange(OTHER_ISBN))
        assertFalse(sut.isbnFlagged)

        sut.onAction(AddEditOrderAction.OnSave)
        assertEquals(1, repository.writes)
    }

    @Test
    fun `an order that was cancelled can be ordered again`() {
        val repository = CachingOrderRepository(order("1", status = OrderStatus.CANCELLED))
        val sut = viewModel(repository)
        sut.fillRequiredFields()
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        sut.onAction(AddEditOrderAction.OnSave)

        assertEquals(1, repository.writes)
    }

    @Test
    fun `a received order still takes its ISBN`() {
        val sut = viewModel(CachingOrderRepository(order("1", status = OrderStatus.RECEIVED)))

        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        assertTrue(sut.isbnFlagged)
    }

    @Test
    fun `a sync taking the ISBN flags it and one freeing it clears the flag`() {
        val repository = CachingOrderRepository()
        val sut = viewModel(repository)
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))
        assertFalse(sut.isbnFlagged)

        repository.orders.value = listOf(order("1"))
        assertTrue(sut.isbnFlagged)

        repository.orders.value = emptyList()
        assertFalse(sut.isbnFlagged)
    }

    @Test
    fun `a new order does not count as its own duplicate once it is saved`() {
        val repository = CachingOrderRepository()
        val sut = viewModel(repository)
        sut.fillRequiredFields()
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        sut.onAction(AddEditOrderAction.OnSave)

        assertEquals(1, repository.writes)
        assertFalse(sut.isbnFlagged)
    }

    @Test
    fun `saving a new order twice writes one order`() {
        val repository = CachingOrderRepository()
        val sut = viewModel(repository)
        sut.fillRequiredFields()
        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))

        sut.onAction(AddEditOrderAction.OnSave)
        sut.onAction(AddEditOrderAction.OnSave)

        assertEquals(1, repository.orders.value.size)
    }

    @Test
    fun `editing an order keeps working with its own ISBN`() {
        val repository = CachingOrderRepository(order("1"))
        val sut = viewModel(repository, AddEditOrderArgs(orderId = "1"))

        sut.onAction(AddEditOrderAction.OnSave)

        assertFalse(sut.isbnFlagged)
        assertEquals(1, repository.writes)
    }

    @Test
    fun `orders that already share an ISBN stay editable`() {
        val repository = CachingOrderRepository(order("1"), order("2"))
        val sut = viewModel(repository, AddEditOrderArgs(orderId = "1"))
        sut.onAction(AddEditOrderAction.OnTitleChange("new title"))

        sut.onAction(AddEditOrderAction.OnSave)

        assertFalse(sut.isbnFlagged)
        assertEquals(
            "new title",
            repository.orders.value
                .first { it.id == "1" }
                .title,
        )
    }

    @Test
    fun `an edit cannot move an order onto another order's ISBN`() {
        val repository = CachingOrderRepository(order("1"), order("2", isbn = OTHER_ISBN))
        val sut = viewModel(repository, AddEditOrderArgs(orderId = "2"))

        sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))
        sut.onAction(AddEditOrderAction.OnSave)

        assertTrue(sut.isbnFlagged)
        assertEquals(0, repository.writes)
    }
}
