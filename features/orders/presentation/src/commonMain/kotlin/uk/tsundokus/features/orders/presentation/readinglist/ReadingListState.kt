package uk.tsundokus.features.orders.presentation.readinglist

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.ReadState

@Stable
data class ReadingListState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val grouped: Map<ReadState, List<Order>> = emptyMap(),
) {
    /** An empty shelf means something different once a search is on: no matches, not nothing owned. */
    val isFiltered: Boolean
        get() = searchQuery.isNotBlank()
}
