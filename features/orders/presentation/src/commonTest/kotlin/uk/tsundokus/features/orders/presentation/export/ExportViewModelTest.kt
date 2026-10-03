package uk.tsundokus.features.orders.presentation.export

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import uk.tsundokus.features.orders.domain.export.ExportFormat
import uk.tsundokus.features.orders.domain.export.ExportedFile
import uk.tsundokus.features.orders.domain.export.OrdersExporter
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.presentation.selection.RecordingOrderRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class RecordingExporter : OrdersExporter {
    val formats = mutableListOf<ExportFormat>()

    override suspend fun export(format: ExportFormat): ExportedFile {
        formats += format
        return ExportedFile(baseName = "tsundoku-orders-2026-10-03", format = format, bytes = ByteArray(0))
    }
}

private val orders =
    listOf(
        Order(id = "a", title = "Berserk 41"),
        Order(id = "b", title = "Berserk 42", status = OrderStatus.CANCELLED),
        Order(id = "c", title = "Akira 1", status = OrderStatus.RECEIVED),
    )

class ExportViewModelTest {
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

    private val exporter = RecordingExporter()

    // Lazy: the ViewModel must start after setUp has swapped in the test Main dispatcher.
    private val sut by lazy { ExportViewModel(RecordingOrderRepository(orders), exporter) }

    @Test
    fun `the screen counts every order and the cancelled ones`() =
        runTest {
            sut.state.test {
                val state = expectMostRecentItem()
                assertEquals(3, state.orderCount)
                assertEquals(1, state.cancelledCount)
                assertTrue(state.canSave)
            }
        }

    @Test
    fun `saving exports the picked format and hands the file over`() =
        runTest {
            sut.state.test {
                sut.onAction(ExportAction.OnFormatSelected(ExportFormat.JSON))
                expectMostRecentItem()

                sut.events.test {
                    sut.onAction(ExportAction.OnSave)
                    val event = assertIs<ExportEvent.SaveFile>(awaitItem())
                    assertEquals(ExportFormat.JSON, event.file.format)
                }
                assertFalse(expectMostRecentItem().canSave)
                assertEquals(listOf(ExportFormat.JSON), exporter.formats)
            }
        }

    @Test
    fun `a second press while saving does nothing`() =
        runTest {
            sut.events.test {
                sut.onAction(ExportAction.OnSave)
                sut.onAction(ExportAction.OnSave)
                awaitItem()
                expectNoEvents()
            }
            assertEquals(1, exporter.formats.size)
        }

    @Test
    fun `a saved file is named back to the user`() =
        runTest {
            sut.state.test {
                sut.events.test {
                    sut.onAction(ExportAction.OnSave)
                    awaitItem()

                    sut.onAction(ExportAction.OnSaveFinished(ExportSaveResult.Saved("orders.csv")))

                    assertIs<ExportEvent.ShowMessage>(awaitItem())
                }
                assertTrue(expectMostRecentItem().canSave)
            }
        }

    @Test
    fun `a dismissed dialog says nothing and allows saving again`() =
        runTest {
            sut.state.test {
                sut.events.test {
                    sut.onAction(ExportAction.OnSave)
                    awaitItem()

                    sut.onAction(ExportAction.OnSaveFinished(ExportSaveResult.Cancelled))

                    expectNoEvents()
                }
                assertTrue(expectMostRecentItem().canSave)
            }
        }

    @Test
    fun `a failed save says so`() =
        runTest {
            sut.events.test {
                sut.onAction(ExportAction.OnSave)
                awaitItem()

                sut.onAction(ExportAction.OnSaveFinished(ExportSaveResult.Failed))

                assertIs<ExportEvent.ShowMessage>(awaitItem())
            }
        }
}
