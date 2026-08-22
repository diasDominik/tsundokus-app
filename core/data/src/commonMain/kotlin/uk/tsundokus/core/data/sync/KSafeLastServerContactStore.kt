package uk.tsundokus.core.data.sync

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.security.SecureStore
import uk.tsundokus.core.domain.sync.LastServerContactStore

@Single(binds = [LastServerContactStore::class])
class KSafeLastServerContactStore(
    secureStore: SecureStore,
) : LastServerContactStore {
    private var stored by secureStore<Long?>(null, key = KEY)
    private val _lastContactAt = MutableStateFlow(stored)

    override val lastContactAt: StateFlow<Long?> = _lastContactAt.asStateFlow()

    override fun record(serverTimeMillis: Long) {
        stored = serverTimeMillis
        _lastContactAt.value = serverTimeMillis
    }

    override fun clear() {
        stored = null
        _lastContactAt.value = null
    }

    private companion object {
        const val KEY = "ordersLastServerContact"
    }
}
