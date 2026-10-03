package uk.tsundokus.features.orders.data.export

import kotlinx.coroutines.flow.first
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.domain.dates.isoFromEpochMillis
import uk.tsundokus.features.orders.domain.export.ExportFormat
import uk.tsundokus.features.orders.domain.export.ExportedFile
import uk.tsundokus.features.orders.domain.export.OrdersExporter
import uk.tsundokus.features.orders.domain.order.OrderRepository
import kotlin.time.Clock

/**
 * Exports the orders on this device — cancelled ones too, with their status — oldest first. The
 * local copy is complete (sync keeps it so) and holds edits not yet synced, so this works offline.
 */
@Single(binds = [OrdersExporter::class])
class LocalOrdersExporter(
    private val orderRepository: OrderRepository,
) : OrdersExporter {
    override suspend fun export(format: ExportFormat): ExportedFile {
        val orders = orderRepository.getOrders().first().sortedBy { it.createdAt }
        val now = Clock.System.now().toEpochMilliseconds()
        val text =
            when (format) {
                ExportFormat.CSV -> ordersToCsv(orders)
                ExportFormat.JSON -> ordersToJson(orders, exportedAtMillis = now)
            }
        return ExportedFile(
            baseName = "tsundoku-orders-${isoFromEpochMillis(now)}",
            format = format,
            bytes = text.encodeToByteArray(),
        )
    }
}
