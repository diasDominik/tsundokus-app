package uk.tsundokus.features.orders.data.sync

import kotlinx.coroutines.flow.Flow
import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.sync.PendingWrites
import uk.tsundokus.features.orders.database.TsundokuDatabase

@Single(binds = [PendingWrites::class])
class OrderPendingWrites(
    private val database: TsundokuDatabase,
) : PendingWrites {
    override fun observeCount(): Flow<Int> = database.pendingOrderOpDao.observeCount()
}
