package uk.tsundokus.features.orders.presentation.readinglist

import androidx.compose.runtime.Stable
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.ReadState

@Stable
data class ReadingListState(
    val isLoading: Boolean = true,
    val grouped: Map<ReadState, List<Order>> = emptyMap(),
)
