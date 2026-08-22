package uk.tsundokus.core.data.auth

import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.auth.SessionInvalidator
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.StaleSession
import uk.tsundokus.core.domain.auth.StaleSessionStore

@Single(binds = [SessionInvalidator::class])
class DefaultSessionInvalidator(
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
) : SessionInvalidator {
    /**
     * Order matters and is not arbitrary. The session is the only place the account identity
     * lives, so it has to be read before it is dropped — and the record has to be *written* first
     * too: a crash between the two steps then leaves a stale record with no session, which reads
     * as an expired session and keeps the cache. Clearing first would lose the record and make the
     * next launch treat the cache as a stranger's.
     */
    override fun invalidate() {
        sessionStorage.get()?.user?.let { user ->
            staleSessionStore.set(
                StaleSession(
                    userId = user.id,
                    email = user.email,
                    username = user.username,
                    userType = user.userType,
                ),
            )
        }
        sessionStorage.set(null)
    }
}
