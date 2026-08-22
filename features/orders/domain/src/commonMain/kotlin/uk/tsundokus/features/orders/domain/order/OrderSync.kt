package uk.tsundokus.features.orders.domain.order

import uk.tsundokus.features.orders.domain.models.Order

/**
 * One delta-sync result: [serverTime] is the new cursor (epoch millis), [changed] the active orders
 * to upsert locally, [deletedIds] the ids to drop.
 */
data class OrderSync(
    val serverTime: Long,
    val changed: List<Order>,
    val deletedIds: List<String>,
)
