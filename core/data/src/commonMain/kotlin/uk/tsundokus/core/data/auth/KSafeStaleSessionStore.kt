package uk.tsundokus.core.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.security.SecureStore
import uk.tsundokus.core.domain.auth.StaleSession
import uk.tsundokus.core.domain.auth.StaleSessionStore

/**
 * Encrypted rather than plaintext because the record holds the user's email address. Mirrors the
 * stored value into a [MutableStateFlow] exactly as [KSafeSessionStorage] does, so the shell can
 * observe it alongside the session.
 */
@Single(binds = [StaleSessionStore::class])
class KSafeStaleSessionStore(
    secureStore: SecureStore,
) : StaleSessionStore {
    private var staleSession by secureStore<StaleSession?>(null, key = KEY)
    private val _state = MutableStateFlow(staleSession)

    override val state: StateFlow<StaleSession?> = _state.asStateFlow()

    override fun get(): StaleSession? = staleSession

    override fun set(session: StaleSession?) {
        staleSession = session
        _state.value = session
    }

    override fun clear() = set(null)

    private companion object {
        const val KEY = "staleSession"
    }
}
