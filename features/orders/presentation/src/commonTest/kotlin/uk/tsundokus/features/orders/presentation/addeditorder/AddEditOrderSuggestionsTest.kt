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

private class StoredOrdersRepository(
    private val orders: List<Order>,
) : OrderRepository {
    override fun getOrders(): Flow<List<Order>> = flowOf(orders)

    override fun getOrderById(id: String): Flow<Order?> = flowOf(orders.firstOrNull { it.id == id })

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

private class StaticPreferencesRepository : AppPreferencesRepository {
    private val currency = MutableStateFlow(AppCurrency.EUR)
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

private fun order(
    id: String,
    title: String,
    author: String = "",
    publisher: String = "",
    store: String = "",
    createdAt: Long = 0L,
) = Order(
    id = id,
    title = title,
    author = author,
    publisher = publisher,
    store = store,
    createdAt = createdAt,
)

class AddEditOrderSuggestionsTest {
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

    private val history =
        listOf(
            order("1", "One Piece", "Eiichiro Oda", "Carlsen", "Hugendubel", createdAt = 1L),
            order("2", "One Piece", "Eiichiro Oda", "Carlsen", "Amazon", createdAt = 2L),
            order("3", "Berserk", "Kentaro Miura", "Panini", "Amazon", createdAt = 3L),
        )

    private fun viewModel(
        orders: List<Order> = history,
        orderId: String? = null,
    ) = AddEditOrderViewModel(
        orderId = orderId,
        orderRepository = StoredOrdersRepository(orders),
        appPreferencesRepository = StaticPreferencesRepository(),
    )

    @Test
    fun `typing part of a value already used offers it`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnAuthorChange("oda"))

            assertEquals(OrderFormField.AUTHOR, sut.state.value.suggestionField)
            assertEquals(listOf("Eiichiro Oda"), sut.state.value.suggestions)
        }

    @Test
    fun `a single character is too little to suggest on`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnAuthorChange("o"))

            assertEquals(null, sut.state.value.suggestionField)
            assertTrue(
                sut.state.value.suggestions
                    .isEmpty(),
            )
        }

    @Test
    fun `the value typed in full has nothing left to complete`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnStoreChange("Amazon"))

            assertTrue(
                sut.state.value.suggestions
                    .isEmpty(),
            )
        }

    @Test
    fun `what starts with the query is offered before what merely contains it`() =
        runTest {
            val sut =
                viewModel(
                    orders =
                        listOf(
                            order("1", "Vinland Saga", store = "Book Depot"),
                            order("2", "Saga", store = "Saga Books"),
                        ),
                )
            sut.onAction(AddEditOrderAction.OnTitleChange("sag"))

            assertEquals(listOf("Saga", "Vinland Saga"), sut.state.value.suggestions)
        }

    @Test
    fun `the most used value is offered first`() =
        runTest {
            val sut = viewModel()
            // Both authors contain "ro" and neither starts with it, so only how often each was
            // used can order them.
            sut.onAction(AddEditOrderAction.OnAuthorChange("ro"))

            assertEquals(listOf("Eiichiro Oda", "Kentaro Miura"), sut.state.value.suggestions)
        }

    @Test
    fun `picking a title fills in the details that series was last recorded with`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnTitleChange("One Pie"))
            sut.onAction(AddEditOrderAction.OnSuggestionSelected("One Piece"))

            val state = sut.state.value
            assertEquals("One Piece", state.title)
            assertEquals("Eiichiro Oda", state.author)
            assertEquals("Carlsen", state.publisher)
            // The newest of the two "One Piece" orders, not the first one recorded.
            assertEquals("Amazon", state.store)
            assertEquals(null, state.suggestionField)
        }

    @Test
    fun `picking a title leaves what the user already typed alone`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnStoreChange("Thalia"))
            sut.onAction(AddEditOrderAction.OnTitleChange("One Pie"))
            sut.onAction(AddEditOrderAction.OnSuggestionSelected("One Piece"))

            assertEquals("Thalia", sut.state.value.store)
        }

    @Test
    fun `picking a suggestion clears the error on the field it fills`() =
        runTest {
            val sut = viewModel()
            sut.onAction(AddEditOrderAction.OnSave)
            assertTrue(OrderFormField.AUTHOR in sut.state.value.errors)

            sut.onAction(AddEditOrderAction.OnAuthorChange("Miu"))
            sut.onAction(AddEditOrderAction.OnSuggestionSelected("Kentaro Miura"))

            assertFalse(OrderFormField.AUTHOR in sut.state.value.errors)
        }

    @Test
    fun `the order being edited is not suggested back to the user`() =
        runTest {
            val sut = viewModel(orderId = "3")
            sut.onAction(AddEditOrderAction.OnAuthorChange("Miu"))

            assertTrue(
                sut.state.value.suggestions
                    .isEmpty(),
            )
        }

    @Test
    fun `loading the history leaves the form untouched`() =
        runTest {
            val sut = viewModel()

            assertFalse(sut.state.value.isDirty)
            assertEquals("", sut.state.value.title)
        }
}
