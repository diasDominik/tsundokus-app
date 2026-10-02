package uk.tsundokus.features.orders.presentation.readinglist

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.presentation.selection.RecordingOrderRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

private val orders =
    listOf(
        Order(id = "a", title = "Vinland Saga 3", status = OrderStatus.RECEIVED, readState = ReadState.WANT),
        Order(id = "b", title = "Berserk 41", status = OrderStatus.RECEIVED, readState = ReadState.READING),
        Order(id = "c", title = "Berserk 42", status = OrderStatus.RECEIVED, readState = ReadState.WANT),
        Order(id = "d", title = "Akira 1", status = OrderStatus.CANCELLED, readState = ReadState.WANT),
    )

class ReadingListSelectionTest {
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

    private val repository = RecordingOrderRepository(orders)

    // Lazy: the ViewModel must start after setUp has swapped in the test Main dispatcher.
    private val sut by lazy { ReadingListViewModel(repository) }

    @Test
    fun `picking all takes only what the search shows and never a cancelled order`() =
        runTest {
            sut.state.test {
                sut.onSearchQueryChange("berserk")
                expectMostRecentItem()

                sut.onPickAllShown()

                assertEquals(setOf("b", "c"), expectMostRecentItem().picked)
            }
        }

    @Test
    fun `marking the picked volumes read offers an undo that restores them`() =
        runTest {
            sut.state.test {
                sut.onTogglePicked("a")
                sut.onTogglePicked("c")
                expectMostRecentItem()

                sut.events.test {
                    sut.onSetPickedReadState(ReadState.READ)
                    assertIs<ReadingListEvent.ShowUndoableMessage>(awaitItem())
                }
                val afterChange = expectMostRecentItem()
                assertNull(afterChange.picked)
                assertEquals(listOf("a", "c"), afterChange.grouped[ReadState.READ]?.map { it.id })

                sut.onUndo()

                assertEquals(orders, repository.stored.value)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a picked order that is already in the state is not written`() =
        runTest {
            sut.state.test {
                sut.onTogglePicked("a")
                sut.onTogglePicked("b")
                expectMostRecentItem()

                sut.onSetPickedReadState(ReadState.READING)

                assertEquals(listOf("a"), repository.updatedBatches.single().map { it.id })
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `stopping clears the picked volumes`() =
        runTest {
            sut.state.test {
                sut.onStartPicking()
                sut.onTogglePicked("a")
                expectMostRecentItem()

                sut.onStopPicking()

                assertNull(expectMostRecentItem().picked)
            }
        }
}
