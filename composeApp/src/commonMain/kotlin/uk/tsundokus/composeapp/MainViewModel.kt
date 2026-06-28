package uk.tsundokus.composeapp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation3.runtime.NavKey
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.preferences.AppPreferencesRepository
import uk.tsundokus.core.domain.preferences.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import org.koin.core.annotation.KoinViewModel

/** Whether the persisted session has been resolved yet, and its outcome. */
enum class SessionState { Loading, Authenticated, Unauthenticated }

@KoinViewModel
class MainViewModel(
    sessionStorage: SessionStorage,
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
