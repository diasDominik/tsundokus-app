package uk.tsundokus.features.orders.presentation.serieslist

sealed interface SeriesListAction {
    data class OnSearchQueryChange(val query: String) : SeriesListAction

    data class OnSortSelected(val sort: SeriesSort) : SeriesListAction

    /** Wide screens: show this series beside the list. */
    data class OnSeriesSelected(val key: String) : SeriesListAction
}
