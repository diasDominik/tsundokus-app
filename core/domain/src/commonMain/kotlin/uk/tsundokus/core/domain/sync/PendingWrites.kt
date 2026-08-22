package uk.tsundokus.core.domain.sync

import kotlinx.coroutines.flow.Flow

/**
 * How many locally-made writes have not reached the server yet — exactly what is lost by signing
 * out or switching accounts. Powers the "N unsynced" indicator.
 */
interface PendingWrites {
    fun observeCount(): Flow<Int>
}
