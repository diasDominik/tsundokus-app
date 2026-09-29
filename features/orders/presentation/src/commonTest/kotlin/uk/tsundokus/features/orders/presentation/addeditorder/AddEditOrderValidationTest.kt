package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

private class FakeAppPreferencesRepository(
    currency: AppCurrency = AppCurrency.EUR,
) : AppPreferencesRepository {
    private val currency = MutableStateFlow(currency)
    private val theme = MutableStateFlow(ThemeMode.SYSTEM)

    override fun themeMode(): Flow<ThemeMode> = theme

    override suspend fun setThemeMode(mode: ThemeMode) {
        theme.value = mode
    }

    override fun currency(): Flow<AppCurrency> = currency

    override suspend fun setCurrency(currency: AppCurrency) {
        this.currency.value = currency
    }
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

    private fun viewModel(
        repository: OrderRepository = RecordingOrderRepository(),
        preferences: AppPreferencesRepository = FakeAppPreferencesRepository(),
        args: AddEditOrderArgs = AddEditOrderArgs(),
    ) = AddEditOrderViewModel(
        args = args,
        orderRepository = repository,
        appPreferencesRepository = preferences,
        bookRepository = FakeBookRepository(),
    )

    private fun AddEditOrderViewModel.fillValidForm() {
        onAction(AddEditOrderAction.OnTitleChange("Berserk"))
        onAction(AddEditOrderAction.OnIsbnChange("9784088820453"))
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
            // Listed rather than OrderFormField.entries: the optional date fields can also carry an
            // error, but only for an impossible sequence — never for being left empty.
            assertEquals(
                setOf(
                    OrderFormField.TITLE,
                    OrderFormField.ISBN,
                    OrderFormField.AUTHOR,
                    OrderFormField.PUBLISHER,
                    OrderFormField.STORE,
                    OrderFormField.PRICE,
                    OrderFormField.ORDER_DATE,
                ),
                sut.state.value.errors,
            )
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

    @Test
    fun `a received date before the order date blocks the save`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnStatusSelected(OrderStatus.RECEIVED))
            sut.onAction(AddEditOrderAction.OnReceivedDateChange("2026-01-01"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals(setOf(OrderFormField.RECEIVED_DATE), sut.state.value.errors)
            assertEquals(null, repository.created)
        }

    @Test
    fun `a ship date before the order date blocks the save`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnShipDateChange("2026-01-01"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals(setOf(OrderFormField.SHIP_DATE), sut.state.value.errors)
        }

    @Test
    fun `a received date before the ship date blocks the save`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnShipDateChange("2026-05-01"))
            sut.onAction(AddEditOrderAction.OnReceivedDateChange("2026-04-01"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals(setOf(OrderFormField.RECEIVED_DATE), sut.state.value.errors)
        }

    @Test
    fun `dates left unset are never impossible`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnSave)

            assertTrue(
                sut.state.value.errors
                    .isEmpty(),
            )
            assertEquals("", repository.created?.shipDate)
        }

    @Test
    fun `moving the order date clears a date error it no longer causes`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnShipDateChange("2026-01-01"))
            sut.onAction(AddEditOrderAction.OnSave)
            assertEquals(setOf(OrderFormField.SHIP_DATE), sut.state.value.errors)

            // The offending value is the *order* date, not the ship date the error is pinned to.
            sut.onAction(AddEditOrderAction.OnOrderDateChange("2025-12-01"))

            assertTrue(
                sut.state.value.errors
                    .isEmpty(),
            )
        }

    @Test
    fun `an ISBN with a wrong check digit blocks the save`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnIsbnChange("9784088820454"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals(setOf(OrderFormField.ISBN), sut.state.value.errors)
            assertEquals(null, repository.created)
        }

    @Test
    fun `a typed ISBN is saved as its ISBN-13 digits`() =
        runTest {
            val repository = RecordingOrderRepository()
            val sut = viewModel(repository)
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnIsbnChange("4-08-882045-2"))
            sut.onAction(AddEditOrderAction.OnSave)

            assertEquals("9784088820453", repository.created?.isbn)
        }

    @Test
    fun `hyphens never reach the ISBN field`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnIsbnChange("978-4-08"))

            assertEquals("978408", sut.state.value.isbn)
        }

    @Test
    fun `a new order from a scan opens with its ISBN and nothing to discard`() =
        runTest {
            val sut = viewModel(args = AddEditOrderArgs(initialIsbn = "9784088820453"))

            assertEquals("9784088820453", sut.state.value.isbn)
            assertFalse(sut.state.value.isDirty)
        }

    @Test
    fun `a yen price reads a separator as grouping`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnCurrencySelected(AppCurrency.fromCode("JPY")))
            sut.onAction(AddEditOrderAction.OnPriceChange("1,200"))

            assertEquals("1200", sut.state.value.price)
        }

    @Test
    fun `switching to a currency without decimals refits the price`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnPriceChange("12.40"))
            sut.onAction(AddEditOrderAction.OnCurrencySelected(AppCurrency.fromCode("JPY")))

            // Rounded to whole yen, not read as 1240.
            assertEquals("12", sut.state.value.price)
        }

    @Test
    fun `a new order starts in the currency chosen in settings`() =
        runTest {
            val sut = viewModel(preferences = FakeAppPreferencesRepository(AppCurrency.GBP))

            assertEquals(AppCurrency.GBP, sut.state.value.currency)
        }

    @Test
    fun `seeding the currency does not count as an unsaved change`() =
        runTest {
            val sut = viewModel(preferences = FakeAppPreferencesRepository(AppCurrency.USD))

            assertFalse(sut.state.value.isDirty)
        }

    @Test
    fun `an untouched form has nothing to discard`() =
        runTest {
            val sut = viewModel()

            assertFalse(sut.state.value.isDirty)
        }

    @Test
    fun `typing into the form marks it dirty`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnTitleChange("Berserk"))

            assertTrue(sut.state.value.isDirty)
        }

    @Test
    fun `undoing every edit by hand leaves nothing to discard`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnTitleChange("Berserk"))
            sut.onAction(AddEditOrderAction.OnTitleChange(""))

            assertFalse(sut.state.value.isDirty)
        }

    @Test
    fun `a failed validation is not an unsaved change on its own`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnSave)

            assertTrue(
                sut.state.value.errors
                    .isNotEmpty(),
            )
            assertFalse(sut.state.value.isDirty)
        }

    @Test
    fun `a saved form has nothing left to discard`() =
        runTest {
            val sut = viewModel()
            sut.fillValidForm()
            sut.onAction(AddEditOrderAction.OnSave)

            assertFalse(sut.state.value.isDirty)
        }
}
