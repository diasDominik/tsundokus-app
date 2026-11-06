package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
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
import kotlin.test.assertTrue

private const val NEW_VOLUME_ISBN = "9783551762023"
private val YEN = AppCurrency.fromCode("JPY")

/** The next volume as the book databases answer for it: known, but without a publisher. */
private val newVolume = BookInfo(isbn = NEW_VOLUME_ISBN, title = "Naruto", author = "Kishimoto", volume = "12")

private val eleventhVolume =
    Order(
        id = "naruto-11",
        title = "Naruto",
        author = "Masashi Kishimoto",
        publisher = "Carlsen",
        volume = "11",
        store = "Thalia",
        price = 7.5,
        status = OrderStatus.RECEIVED,
        createdAt = 2,
    )

private class SeriesOrdersRepository(
    private val orders: List<Order>,
) : OrderRepository {
    override fun getOrders(): Flow<List<Order>> = flowOf(orders)

    override fun getOrderById(id: String): Flow<Order?> = flowOf(orders.firstOrNull { it.id == id })

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = Result.Success(order)

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = Result.Success(order)

    override suspend fun deleteOrder(id: String): EmptyResult<DataError.Remote> = Result.Success(Unit)

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

private class SeriesTestPreferences(
    private val currencyFlow: Flow<AppCurrency> = flowOf(AppCurrency.EUR),
) : AppPreferencesRepository {
    override fun themeMode(): Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override fun currency(): Flow<AppCurrency> = currencyFlow

    override suspend fun setCurrency(currency: AppCurrency) = Unit
}

class AddEditOrderSeriesDetailsTest {
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
        orders: List<Order> = listOf(eleventhVolume),
        book: BookInfo = newVolume,
        args: AddEditOrderArgs = AddEditOrderArgs(),
        preferences: AppPreferencesRepository = SeriesTestPreferences(),
    ) = AddEditOrderViewModel(
        args = args,
        orderRepository = SeriesOrdersRepository(orders),
        appPreferencesRepository = preferences,
        bookRepository = FakeBookRepository(mapOf(book.isbn to book)),
    )

    @Test
    fun `scanning a new volume fills the publisher shop and price from its series`() =
        runTest {
            val sut = viewModel()

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            val state = sut.state.value
            assertEquals("Carlsen", state.publisher)
            assertEquals("Thalia", state.store)
            assertEquals("7.50", state.price)
            assertEquals(AppCurrency.EUR, state.currency)
        }

    @Test
    fun `an order scanned from the start of the form is filled in on open`() =
        runTest {
            val sut = viewModel(args = AddEditOrderArgs(initialIsbn = NEW_VOLUME_ISBN))

            assertEquals("Carlsen", sut.state.value.publisher)
            assertEquals("7.50", sut.state.value.price)
        }

    @Test
    fun `what the book databases know wins over the series`() =
        runTest {
            val sut = viewModel(book = newVolume.copy(publisher = "Tokyopop"))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("Tokyopop", sut.state.value.publisher)
            assertEquals("Kishimoto", sut.state.value.author)
            assertEquals("Thalia", sut.state.value.store)
        }

    @Test
    fun `what the user typed is not overwritten by the series`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnStoreChange("Hugendubel"))
            sut.onAction(AddEditOrderAction.OnPriceChange("9"))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("Hugendubel", sut.state.value.store)
            assertEquals("9", sut.state.value.price)
            assertEquals("Carlsen", sut.state.value.publisher)
        }

    @Test
    fun `a book from no known series fills nothing more`() =
        runTest {
            val sut = viewModel(book = newVolume.copy(title = "Something else"))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("", sut.state.value.publisher)
            assertEquals("", sut.state.value.store)
            assertEquals("", sut.state.value.price)
        }

    @Test
    fun `a price from the series arrives in the currency it was paid in`() =
        runTest {
            val sut = viewModel(orders = listOf(eleventhVolume.copy(price = 700.0, currency = YEN)))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("700", sut.state.value.price)
            assertEquals(YEN, sut.state.value.currency)
        }

    @Test
    fun `a currency the user picked keeps a price in another currency out`() =
        runTest {
            val sut = viewModel(orders = listOf(eleventhVolume.copy(price = 700.0, currency = YEN)))
            sut.onAction(AddEditOrderAction.OnCurrencySelected(AppCurrency.fromCode("GBP")))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("", sut.state.value.price)
            assertEquals("GBP", sut.state.value.currency.code)
            assertEquals("Thalia", sut.state.value.store)
        }

    @Test
    fun `a currency the user picked takes the price when the series paid in it too`() =
        runTest {
            val sut = viewModel(orders = listOf(eleventhVolume.copy(price = 700.0, currency = YEN)))
            sut.onAction(AddEditOrderAction.OnCurrencySelected(YEN))

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("700", sut.state.value.price)
        }

    @Test
    fun `the settings currency arriving late does not relabel a series price`() =
        runTest {
            val gate = CompletableDeferred<Unit>()
            val lateCurrency =
                flow {
                    gate.await()
                    emit(AppCurrency.EUR)
                }
            val sut =
                viewModel(
                    orders = listOf(eleventhVolume.copy(price = 700.0, currency = YEN)),
                    preferences = SeriesTestPreferences(lateCurrency),
                )
            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            gate.complete(Unit)

            assertEquals("700", sut.state.value.price)
            assertEquals(YEN, sut.state.value.currency)
        }

    @Test
    fun `editing an order takes no price from its series`() =
        runTest {
            val unpriced =
                Order(
                    id = "naruto-12",
                    title = "Naruto",
                    author = "Kishimoto",
                    publisher = "Carlsen",
                    store = "Thalia",
                    orderDate = "2026-09-01",
                    createdAt = 3,
                )
            val sut =
                viewModel(
                    orders = listOf(eleventhVolume, unpriced),
                    args = AddEditOrderArgs(orderId = unpriced.id),
                )

            sut.onAction(AddEditOrderAction.OnIsbnScanned(NEW_VOLUME_ISBN))

            assertEquals("", sut.state.value.price)
        }

    @Test
    fun `picking a title fills the price too and clears the errors of what it filled`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnSave)
            assertTrue(OrderFormField.PRICE in sut.state.value.errors)
            assertTrue(OrderFormField.STORE in sut.state.value.errors)

            sut.onAction(AddEditOrderAction.OnTitleChange("Naru"))
            sut.onAction(AddEditOrderAction.OnSuggestionSelected("Naruto"))

            val state = sut.state.value
            assertEquals("7.50", state.price)
            assertFalse(OrderFormField.PRICE in state.errors)
            assertFalse(OrderFormField.STORE in state.errors)
        }

    @Test
    fun `the next volume opens with the series price and nothing to discard`() =
        runTest {
            val sut =
                viewModel(
                    args =
                        AddEditOrderArgs(
                            prefill = OrderPrefill(title = "Naruto", currencyCode = "EUR", price = "7.50"),
                        ),
                )

            assertEquals("7.50", sut.state.value.price)
            assertFalse(sut.state.value.isDirty)
        }
}
