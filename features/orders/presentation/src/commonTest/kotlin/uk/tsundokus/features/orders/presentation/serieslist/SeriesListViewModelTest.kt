package uk.tsundokus.features.orders.presentation.serieslist

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
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.order.OrderRepository
import uk.tsundokus.features.orders.domain.series.toSeries
import uk.tsundokus.features.orders.presentation.addeditorder.OrderPrefill
import uk.tsundokus.features.orders.presentation.seriesdetail.nextOrderPrefill
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class StoredOrders(
    orders: List<Order>,
) : OrderRepository {
    private val orders = MutableStateFlow(orders)

    override fun getOrders(): Flow<List<Order>> = orders

    override fun getOrderById(id: String): Flow<Order?> = orders.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun fetchOrders(): EmptyResult<DataError.Remote> = Result.Success(Unit)

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

private fun order(
    title: String,
    volume: Int,
    createdAt: Long,
) = Order(
    id = "$title-$volume",
    title = title,
    volume = "Vol. $volume",
    status = OrderStatus.RECEIVED,
    createdAt = createdAt,
)

class SeriesListViewModelTest {
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

    // Berserk: newest activity. One Piece: two gaps. Akira: first by title.
    private val orders =
        listOf(
            order("One Piece", 1, createdAt = 1),
            order("One Piece", 4, createdAt = 2),
            order("Berserk", 1, createdAt = 9),
            order("Akira", 1, createdAt = 3),
        )

    @Test
    fun `series are listed most recent first`() =
        runTest {
            val sut = SeriesListViewModel(StoredOrders(orders))

            sut.state.test {
                assertEquals(
                    listOf("Berserk", "Akira", "One Piece"),
                    expectMostRecentItem().series.map { it.title },
                )
            }
        }

    @Test
    fun `series can be sorted by title or by what is missing`() =
        runTest {
            val sut = SeriesListViewModel(StoredOrders(orders))

            sut.state.test {
                sut.onAction(SeriesListAction.OnSortSelected(SeriesSort.TITLE))
                assertEquals(
                    listOf("Akira", "Berserk", "One Piece"),
                    expectMostRecentItem().series.map { it.title },
                )

                sut.onAction(SeriesListAction.OnSortSelected(SeriesSort.MISSING))
                assertEquals("One Piece", expectMostRecentItem().series.first().title)
            }
        }

    @Test
    fun `a search narrows the list`() =
        runTest {
            val sut = SeriesListViewModel(StoredOrders(orders))

            sut.state.test {
                sut.onAction(SeriesListAction.OnSearchQueryChange("ber"))
                assertEquals(listOf("Berserk"), expectMostRecentItem().series.map { it.title })
            }
        }

    @Test
    fun `selecting a series remembers it for the side pane`() =
        runTest {
            val sut = SeriesListViewModel(StoredOrders(orders))

            sut.state.test {
                sut.onAction(SeriesListAction.OnSeriesSelected("berserk"))
                assertEquals("berserk", expectMostRecentItem().selectedKey)
            }
        }

    @Test
    fun `ordering the next volume carries the series details over`() {
        val latest =
            order("One Piece", 12, createdAt = 5).copy(
                author = "Eiichiro Oda",
                publisher = "Carlsen",
                store = "Hugendubel",
                currency = AppCurrency.fromCode("JPY"),
            )
        val series = listOf(order("One Piece", 11, createdAt = 1), latest).toSeries().single()

        assertEquals(
            OrderPrefill(
                title = "One Piece",
                author = "Eiichiro Oda",
                publisher = "Carlsen",
                store = "Hugendubel",
                volume = "Vol. 13",
                currencyCode = "JPY",
            ),
            series.nextOrderPrefill(),
        )
    }
}
