package uk.tsundokus.features.orders.presentation.serieslist

import kotlin.test.Test
import kotlin.test.assertEquals

class MissingRunsTest {
    @Test
    fun `consecutive volumes collapse into one run`() {
        assertEquals(listOf(1..8, 10..11), listOf(1, 2, 3, 4, 5, 6, 7, 8, 10, 11).toRuns())
    }

    @Test
    fun `a lone volume is a run of one`() {
        assertEquals(listOf(3..3, 7..7), listOf(3, 7).toRuns())
    }

    @Test
    fun `nothing missing is no runs`() {
        assertEquals(emptyList(), emptyList<Int>().toRuns())
    }
}
