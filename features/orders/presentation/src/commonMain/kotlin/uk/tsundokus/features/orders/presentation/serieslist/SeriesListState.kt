package uk.tsundokus.features.orders.presentation.serieslist

import androidx.compose.runtime.Stable
import org.jetbrains.compose.resources.StringResource
import tsundokuapp.features.orders.presentation.generated.resources.Res
import tsundokuapp.features.orders.presentation.generated.resources.series_sort_missing
import tsundokuapp.features.orders.presentation.generated.resources.series_sort_recent
import tsundokuapp.features.orders.presentation.generated.resources.series_sort_title
import uk.tsundokus.features.orders.domain.series.Series

@Stable
data class SeriesListState(
    val isLoading: Boolean = true,
    val searchQuery: String = "",
    val sort: SeriesSort = SeriesSort.RECENT,
    val series: List<Series> = emptyList(),
    /** The series shown beside the list on wide screens; null shows the placeholder. */
    val selectedKey: String? = null,
) {
    /** An empty list means something different once a search is on: no matches, not nothing owned. */
    val isFiltered: Boolean get() = searchQuery.isNotBlank()
}

enum class SeriesSort(
    val labelRes: StringResource,
) {
    /** Where something last happened first: the series you are collecting right now. */
    RECENT(Res.string.series_sort_recent),
    TITLE(Res.string.series_sort_title),

    /** Most gaps first: the series to complete. */
    MISSING(Res.string.series_sort_missing),
}
