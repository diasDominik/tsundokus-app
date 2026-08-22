package uk.tsundokus.features.authentication.testing

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import uk.tsundokus.core.domain.auth.StaleSession
import uk.tsundokus.core.domain.auth.StaleSessionStore

class FakeStaleSessionStore(
    initial: StaleSession? = null,
) : StaleSessionStore {
    private val _state = MutableStateFlow(initial)

    override val state: StateFlow<StaleSession?> = _state.asStateFlow()

    override fun get(): StaleSession? = _state.value

    override fun set(session: StaleSession?) {
        _state.value = session
    }

    override fun clear() = set(null)
}
