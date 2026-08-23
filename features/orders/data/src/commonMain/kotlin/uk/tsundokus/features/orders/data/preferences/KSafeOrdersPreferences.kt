package uk.tsundokus.features.orders.data.preferences

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.features.orders.domain.models.OrderSort
import uk.tsundokus.features.orders.domain.models.SortDirection
import uk.tsundokus.features.orders.domain.preferences.OrderSortPreference
import uk.tsundokus.features.orders.domain.preferences.OrdersPreferences

@Single(binds = [OrdersPreferences::class])
class KSafeOrdersPreferences(
    @Named("prefs") private val prefs: KSafe,
) : OrdersPreferences {
    override fun sort(): Flow<OrderSortPreference> =
        combine(
            prefs.getFlow(KEY_SORT, OrderSort.RECENT.name),
            prefs.getFlow(KEY_SORT_DIRECTION, OrderSort.RECENT.defaultDirection.name),
        ) { storedSort, storedDirection ->
            // An unreadable value (renamed enum, corrupted write) falls back rather than throwing:
            // a sort preference is never worth failing the list over.
            val sort = OrderSort.entries.firstOrNull { it.name == storedSort } ?: OrderSort.RECENT
            val direction =
                SortDirection.entries.firstOrNull { it.name == storedDirection } ?: sort.defaultDirection
            OrderSortPreference(sort = sort, direction = direction)
        }

    override suspend fun setSort(preference: OrderSortPreference) {
        prefs.put(KEY_SORT, preference.sort.name, KSafeWriteMode.Plain)
        prefs.put(KEY_SORT_DIRECTION, preference.direction.name, KSafeWriteMode.Plain)
    }

    private companion object {
        private const val KEY_SORT = "ordersSort"
        private const val KEY_SORT_DIRECTION = "ordersSortDirection"
    }
}
