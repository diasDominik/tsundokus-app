package uk.tsundokus.features.orders.presentation.stats

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
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.stats.StatsPeriod
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class StoredOrders(
    orders: List<Order>,
    private val fetchResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : OrderRepository {
    private val orders = MutableStateFlow(orders)

    override fun getOrders(): Flow<List<Order>> = orders

    override fun getOrderById(id: String): Flow<Order?> = orders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = fetchResult

    override suspend fun createOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

    override suspend fun updateOrder(order: Order): Result<Order, DataError.Remote> = error("unused")

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

private class StaticPreferencesRepository(
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

private val YEN = AppCurrency(code = "JPY", symbol = "¥", decimals = 0, displayName = "Japanese Yen")

private fun order(
    id: String,
    price: Double,
    orderDate: String,
    currency: AppCurrency = AppCurrency.EUR,
) = Order(
    id = id,
    title = "Berserk $id",
    price = price,
    currency = currency,
    orderDate = orderDate,
    status = OrderStatus.RECEIVED,
)

class StatsViewModelTest {
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

    // One order today, one well over two years ago: only all time reaches the old one.
    private val orders =
        listOf(
            order("new", price = 10.0, orderDate = todayIso()),
            order("old", price = 30.0, orderDate = isoPlusDays(todayIso(), -800)!!),
        )

    @Test
    fun `stats start on the last twelve months`() =
        runTest {
            val sut = StatsViewModel(StoredOrders(orders), StaticPreferencesRepository())

            sut.state.test {
                val state = expectMostRecentItem()
                assertEquals(StatsPeriod.LAST_12_MONTHS, state.period)
                assertEquals(
                    10.0,
                    state.stats
                        ?.spend
                        ?.single()
                        ?.total,
                )
                assertEquals(2, state.stats?.collection?.owned)
            }
        }

    @Test
    fun `picking a period works the stats out again`() =
        runTest {
            val sut = StatsViewModel(StoredOrders(orders), StaticPreferencesRepository())

            sut.state.test {
                sut.onAction(StatsAction.OnPeriodSelected(StatsPeriod.ALL_TIME))
                val state = expectMostRecentItem()
                assertEquals(StatsPeriod.ALL_TIME, state.period)
                assertEquals(
                    40.0,
                    state.stats
                        ?.spend
                        ?.single()
                        ?.total,
                )
            }
        }

    @Test
    fun `the chosen currency is listed first`() =
        runTest {
            val mixed = orders + order("yen", price = 900.0, orderDate = todayIso(), currency = YEN)
            val sut = StatsViewModel(StoredOrders(mixed), StaticPreferencesRepository(YEN))

            sut.state.test {
                assertEquals(listOf("JPY", "EUR"), expectMostRecentItem().stats?.spend?.map { it.currency.code })
            }
        }

    @Test
    fun `a failed refresh shows a message and keeps the cached stats`() =
        runTest {
            val sut =
                StatsViewModel(
                    StoredOrders(orders, fetchResult = Result.Failure(DataError.Remote.NO_INTERNET)),
                    StaticPreferencesRepository(),
                )

            sut.events.test {
                assertIs<StatsEvent.ShowMessage>(awaitItem())
            }
            sut.state.test {
                assertTrue(expectMostRecentItem().stats?.isEmpty == false)
            }
        }
}
