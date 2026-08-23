package uk.tsundokus.features.orders.domain.preferences

import kotlinx.coroutines.flow.Flow
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.SortDirection

/**
 * Local, device-scoped preferences owned by the orders feature.
 *
 * Sort order is a durable choice — how someone likes to read their own shelf — so it outlives the
 * screen and the app process. The search query and status filter deliberately do *not* live here:
 * they are transient narrowings, and restoring one on launch would look like missing data.
 */
interface OrdersPreferences {
    fun sort(): Flow<OrderSortPreference>

    suspend fun setSort(preference: OrderSortPreference)
}

data class OrderSortPreference(
    val sort: OrderSort = OrderSort.RECENT,
    val direction: SortDirection = OrderSort.RECENT.defaultDirection,
)
