package uk.tsundokus.features.orders.presentation.readinglist

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.ReadState

@Stable
data class ReadingListState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val grouped: Map<ReadState, List<Order>> = emptyMap(),
    /** Every order on the shelf, whatever the search; what picked ids are looked up in. */
    val shelf: List<Order> = emptyList(),
    /** The orders picked to act on together; null while not picking. Only ids still on the shelf. */
    val picked: Set<String>? = null,
) {
    val isPicking: Boolean get() = picked != null

    val shown: List<Order> get() = grouped.values.flatten()

    /** An empty shelf means something different once a search is on: no matches, not nothing owned. */
    val isFiltered: Boolean
        get() = searchQuery.isNotBlank()
}
