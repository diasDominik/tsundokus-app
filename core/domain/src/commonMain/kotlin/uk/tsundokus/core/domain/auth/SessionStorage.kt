package uk.tsundokus.core.domain.auth

import kotlinx.coroutines.flow.StateFlow

/**
 * Abstraction for reading and writing the persisted [AuthInfo].
 *
 * [authState] emits the current session reactively so observers
 * (e.g. the main UI) can respond to login/logout immediately.
 */
interface SessionStorage {
    val authState: StateFlow<AuthInfo?>

    fun get(): AuthInfo?

    fun set(info: AuthInfo?)

    /**
     * Cache-aware restore of the persisted session. Awaits the secure store's
     * cache (which warms asynchronously on some platforms, e.g. web) before
     * reading, then publishes the result to [authState]. Call once on startup
     * to decide the initial screen — [get] can return null before the cache is
     * warm, which would otherwise drop a logged-in user back to sign-in.
     */
    suspend fun load(): AuthInfo?
}
