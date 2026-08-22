package uk.tsundokus.features.orders.database.entities

import androidx.room3.Entity
import androidx.room3.PrimaryKey

/**
 * A local write that has not reached the server yet.
 *
 * Collapsed by design: the whole current order state already lives in [OrderEntity], so an outbox
 * row only records *which* order is dirty and *how* to replay it — [Type.UPSERT] (create or edit,
 * idempotent on the client-minted id) or [Type.DELETE]. Keyed by [orderId] so re-enqueuing the same
 * order coalesces to one pending write instead of piling up per edit.
 */
@Entity(tableName = "pending_order_ops")
data class PendingOrderOpEntity(
    @PrimaryKey val orderId: String,
    val type: String,
    val createdAt: Long,
) {
    enum class Type { UPSERT, DELETE }
}
