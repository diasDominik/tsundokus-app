package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.book.BookInfo
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

private const val ISBN = "9784088820453"
private const val OTHER_ISBN = "9780439420891"

private val book =
    BookInfo(
        isbn = ISBN,
        title = "姫様\"拷問\"の時間です",
        author = "春原ロビンソン, ひらけい",
        publisher = "集英社",
        volume = "1",
        releaseDate = "2019-09-04",
        hasCover = true,
    )

private class SavingOrderRepository(
    private val existing: Order? = null,
) : OrderRepository {
    var saved: Order? = null

    override fun getOrders(): Flow<List<Order>> = flowOf(emptyList())

    override fun getOrderById(id: String): Flow<Order?> = flowOf(existing)

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> {
        saved = order
        return Result.Success(order)
    }

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = createOrder(order)

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

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

private object EuroPreferences : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override fun currency(): Flow<AppCurrency> = flowOf(AppCurrency.EUR)

    override suspend fun setCurrency(currency: AppCurrency) = Unit
}

/** Runs on a StandardTestDispatcher so the typing pause can be stepped through with virtual time. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddEditOrderBookLookupTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        books: FakeBookRepository = FakeBookRepository(mapOf(ISBN to book)),
        orders: OrderRepository = SavingOrderRepository(),
        args: AddEditOrderArgs = AddEditOrderArgs(),
    ) = AddEditOrderViewModel(
        args = args,
        orderRepository = orders,
        appPreferencesRepository = EuroPreferences,
        bookRepository = books,
    )

    private fun TestScope.settle() = advanceUntilIdle()

    @Test
    fun `a scanned ISBN fills the blank fields`() =
        runTest(dispatcher) {
            val sut = viewModel()
            settle()

            sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))
            settle()

            val state = sut.state.value
            assertEquals(book.title, state.title)
            assertEquals(book.author, state.author)
            assertEquals(book.publisher, state.publisher)
            assertEquals("1", state.volume)
            assertEquals("2019-09-04", state.releaseDate)
            assertEquals(ISBN, state.coverIsbn)
        }

    @Test
    fun `what the user typed is never overwritten`() =
        runTest(dispatcher) {
            val sut = viewModel()
            settle()
            sut.onAction(AddEditOrderAction.OnTitleChange("My own title"))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))
            settle()

            assertEquals("My own title", sut.state.value.title)
            assertEquals(book.author, sut.state.value.author)
        }

    @Test
    fun `a typed ISBN waits for a pause before it is looked up`() =
        runTest(dispatcher) {
            val books = FakeBookRepository(mapOf(ISBN to book))
            val sut = viewModel(books)
            settle()

            sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))
            advanceTimeBy(300)
            runCurrent()
            assertTrue(books.lookedUp.isEmpty())

            advanceTimeBy(200)
            runCurrent()
            assertEquals(listOf(ISBN), books.lookedUp)
        }

    @Test
    fun `typing on cancels the lookup for the ISBN before`() =
        runTest(dispatcher) {
            val books = FakeBookRepository(mapOf(ISBN to book))
            val sut = viewModel(books)
            settle()

            // The first ten digits of this ISBN-13 are a valid ISBN-10 of another book.
            sut.onAction(AddEditOrderAction.OnIsbnChange("4088820452"))
            advanceTimeBy(100)
            sut.onAction(AddEditOrderAction.OnIsbnChange(ISBN))
            settle()

            assertEquals(listOf(ISBN), books.lookedUp)
        }

    @Test
    fun `an answer for an ISBN no longer in the field is dropped`() =
        runTest(dispatcher) {
            val gate = CompletableDeferred<Unit>()
            val books = FakeBookRepository(mapOf(ISBN to book), gate = gate)
            val sut = viewModel(books)
            settle()
            sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))
            runCurrent()
            assertTrue(sut.state.value.isLookingUpBook)

            // The lookup is already under way when the field is cleared.
            sut.onAction(AddEditOrderAction.OnIsbnChange(""))
            gate.complete(Unit)
            settle()

            assertEquals("", sut.state.value.title)
            assertFalse(sut.state.value.isLookingUpBook)
        }

    @Test
    fun `a failed lookup leaves the form as it was`() =
        runTest(dispatcher) {
            val sut = viewModel(FakeBookRepository(failing = true))
            settle()

            sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))
            settle()

            assertEquals("", sut.state.value.title)
            assertFalse(sut.state.value.isLookingUpBook)
        }

    @Test
    fun `an order started from a scan is filled in on open`() =
        runTest(dispatcher) {
            val sut = viewModel(args = AddEditOrderArgs(initialIsbn = ISBN))
            settle()

            assertEquals(book.title, sut.state.value.title)
        }

    @Test
    fun `a new order saves with the cover the lookup found`() =
        runTest(dispatcher) {
            val orders = SavingOrderRepository()
            val sut = viewModel(orders = orders)
            settle()
            sut.onAction(AddEditOrderAction.OnIsbnScanned(ISBN))
            settle()
            sut.onAction(AddEditOrderAction.OnStoreChange("Amazon"))
            sut.onAction(AddEditOrderAction.OnPriceChange("7"))
            sut.onAction(AddEditOrderAction.OnOrderDateChange("2026-09-01"))

            sut.onAction(AddEditOrderAction.OnSave)
            settle()

            assertEquals(true, orders.saved?.hasCover)
        }

    @Test
    fun `editing an order keeps its cover until its ISBN changes`() =
        runTest(dispatcher) {
            val existing =
                Order(
                    id = "1",
                    title = "t",
                    author = "a",
                    publisher = "p",
                    store = "s",
                    price = 1.0,
                    orderDate = "2026-09-01",
                    isbn = ISBN,
                    hasCover = true,
                )
            val orders = SavingOrderRepository(existing)
            val sut =
                viewModel(books = FakeBookRepository(), orders = orders, args = AddEditOrderArgs(orderId = "1"))
            settle()
            assertEquals(ISBN, sut.state.value.coverIsbn)

            sut.onAction(AddEditOrderAction.OnSave)
            settle()
            assertEquals(true, orders.saved?.hasCover)

            sut.onAction(AddEditOrderAction.OnIsbnScanned(OTHER_ISBN))
            settle()
            sut.onAction(AddEditOrderAction.OnSave)
            settle()
            assertEquals(false, orders.saved?.hasCover)
        }

    @Test
    fun `an unknown book changes nothing`() =
        runTest(dispatcher) {
            val sut = viewModel(FakeBookRepository())
            settle()

            sut.onAction(AddEditOrderAction.OnIsbnScanned(OTHER_ISBN))
            settle()

            assertEquals("", sut.state.value.title)
            assertNull(sut.state.value.coverIsbn)
        }
}
