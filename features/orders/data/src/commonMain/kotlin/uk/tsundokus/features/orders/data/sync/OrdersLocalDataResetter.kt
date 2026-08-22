package uk.tsundokus.features.orders.data.sync

import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.sync.LastServerContactStore
import uk.tsundokus.core.domain.sync.LocalDataResetter
import uk.tsundokus.core.domain.sync.SyncCursorStore
import uk.tsundokus.features.orders.database.TsundokuDatabase

@Single(binds = [LocalDataResetter::class])
class OrdersLocalDataResetter(
    private val database: TsundokuDatabase,
    private val syncCursorStore: SyncCursorStore,
    private val lastServerContactStore: LastServerContactStore,
) : LocalDataResetter {
    override suspend fun resetLocalData() {
        // Cache, queued writes and sync watermarks all belong to the account that just left; a
        // leftover outbox op would otherwise replay under whoever signs in next.
        database.orderDao.clear()
        database.pendingOrderOpDao.clear()
        syncCursorStore.clear()
        lastServerContactStore.clear()
    }
}
