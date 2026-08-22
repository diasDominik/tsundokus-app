package uk.tsundokus.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.StaleSessionStore
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.sync.LocalDataResetter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.core.annotation.KoinViewModel

/** Whether the persisted session has been resolved yet, and its outcome. */
enum class SessionState { Loading, Authenticated, Unauthenticated }

@KoinViewModel
class MainViewModel(
    private val sessionStorage: SessionStorage,
    private val staleSessionStore: StaleSessionStore,
    private val localDataResetter: LocalDataResetter,
    appPreferencesRepository: AppPreferencesRepository,
) : ViewModel() {
    // Stays Loading until the cache-aware restore resolves, so the UI never
    // defaults to sign-in before the persisted session is known.
    val sessionState: StateFlow<SessionState> =
        flow {
            sessionStorage.load()
            emitAll(
                sessionStorage.authState.map {
                    if (it != null) SessionState.Authenticated else SessionState.Unauthenticated
                },
            )
        }.stateIn(viewModelScope, SharingStarted.Eagerly, SessionState.Loading)

    val isLoggedIn: StateFlow<Boolean> = sessionStorage.authState.map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get() != null)

    val accountName: StateFlow<String> = sessionStorage.authState.map { it?.user?.username.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get()?.user?.username.orEmpty())

    val accountEmail: StateFlow<String> = sessionStorage.authState.map { it?.user?.email.orEmpty() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, sessionStorage.get()?.user?.email.orEmpty())

    val themeMode: StateFlow<ThemeMode> =
        appPreferencesRepository
            .themeMode()
            .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /**
     * A deliberate sign-out or account deletion: the local order cache must not outlive it for the
     * next account on this device. Clearing the stale record marks this as an intentional end (not
     * an expiry), so a later sign-in does not treat the — now empty — cache as recoverable.
     */
    fun onDeliberateSignOut() {
        viewModelScope.launch {
            staleSessionStore.clear()
            localDataResetter.resetLocalData()
        }
    }

    /**
     * After a successful sign-in, decide what to do with the cache an expired session left behind.
     * Same account: keep it, still theirs. Different account: wipe it before they see it. No stale
     * record: nothing to reconcile. Either way the record is cleared — the session is live again.
     */
    fun reconcileAfterLogin() {
        viewModelScope.launch {
            val currentUserId = sessionStorage.get()?.user?.id
            val stale = staleSessionStore.get()
            if (stale != null && currentUserId != null && stale.userId != currentUserId) {
                localDataResetter.resetLocalData()
            }
            staleSessionStore.clear()
        }
    }

    private var pendingPostAuthNavKey: NavKey? = null

    fun setPendingPostAuthNavKey(navKey: NavKey?) {
        pendingPostAuthNavKey = navKey
    }

    fun consumePendingPostAuthNavKey(): NavKey? {
        val value = pendingPostAuthNavKey
        pendingPostAuthNavKey = null
        return value
    }
}
