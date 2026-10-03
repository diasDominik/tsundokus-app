package uk.tsundokus.features.orders.data.export

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import uk.tsundokus.features.orders.domain.models.Order
import kotlin.time.Instant

/**
 * The JSON export: a document naming its [format] and [version], so an import can tell a file is
 * ours and read old ones. Its own model rather than the API's, so the file stays the same when the
 * API changes.
 */
@Serializable
internal data class OrdersExportDocument(
    val format: String = FORMAT_NAME,
    val version: Int = FORMAT_VERSION,
    val exportedAt: String,
    val orders: List<ExportedOrder>,
)

/** One order. Text and dates that are not set are null rather than empty. */
@Serializable
internal data class ExportedOrder(
    val id: String,
    val title: String,
    val volume: String?,
    val author: String?,
    val publisher: String?,
    val isbn: String?,
    val store: String?,
    val price: Double,
    val currency: String,
    val status: String,
    val readState: String,
    val orderDate: String?,
    val releaseDate: String?,
    val shipDate: String?,
    val eta: String?,
    val receivedDate: String?,
    val delayedTo: String?,
    val addedAt: String?,
)

internal const val FORMAT_NAME = "tsundoku-orders"
internal const val FORMAT_VERSION = 1

internal val exportJson =
    Json {
        prettyPrint = true
        encodeDefaults = true
    }

internal fun ordersToJson(
    orders: List<Order>,
    exportedAtMillis: Long,
): String =
    exportJson.encodeToString(
        OrdersExportDocument(
            exportedAt = Instant.fromEpochMilliseconds(exportedAtMillis).toString(),
            orders = orders.map(Order::toExported),
        ),
    )

private fun Order.toExported(): ExportedOrder =
    ExportedOrder(
        id = id,
        title = title,
        volume = volume.ifBlank { null },
        author = author.ifBlank { null },
        publisher = publisher.ifBlank { null },
        isbn = isbn.ifBlank { null },
        store = store.ifBlank { null },
        price = price,
        currency = currency.code,
        status = status.name,
        readState = readState.name,
        orderDate = orderDate.ifBlank { null },
        releaseDate = releaseDate.ifBlank { null },
        shipDate = shipDate.ifBlank { null },
        eta = eta.ifBlank { null },
        receivedDate = receivedDate.ifBlank { null },
        delayedTo = delayedTo.ifBlank { null },
        addedAt = addedAtIso(createdAt).ifBlank { null },
    )

/** When the order was added, as an ISO instant; blank for orders from before that was recorded. */
internal fun addedAtIso(createdAtMillis: Long): String =
    if (createdAtMillis > 0) Instant.fromEpochMilliseconds(createdAtMillis).toString() else ""
