package uk.tsundokus.core.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.security.SecureStore
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.SessionStorage

@Single(binds = [SessionStorage::class])
class KSafeSessionStorage(
    secureStore: SecureStore,
) : SessionStorage {
    private var authInfo by secureStore<AuthInfo?>(null, key = "authInfo")
    private val _authState = MutableStateFlow(authInfo)

    override val authState: StateFlow<AuthInfo?> = _authState.asStateFlow()

    override fun get(): AuthInfo? = authInfo

    override fun set(info: AuthInfo?) {
        authInfo = info
        _authState.value = info
    }
}
