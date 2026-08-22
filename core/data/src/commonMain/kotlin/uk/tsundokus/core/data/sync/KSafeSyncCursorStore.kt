package uk.tsundokus.core.data.sync

import org.koin.core.annotation.Single
import uk.tsundokus.core.data.security.SecureStore
import uk.tsundokus.core.domain.sync.SyncCursorStore

@Single(binds = [SyncCursorStore::class])
class KSafeSyncCursorStore(
    secureStore: SecureStore,
) : SyncCursorStore {
    private var cursor by secureStore<Long?>(null, key = KEY)

    override fun get(): Long? = cursor

    override fun set(cursor: Long) {
        this.cursor = cursor
    }

    override fun clear() {
        cursor = null
    }

    private companion object {
        const val KEY = "ordersSyncCursor"
    }
}
