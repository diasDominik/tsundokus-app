package uk.tsundokus.core.domain.sync

import kotlinx.coroutines.flow.StateFlow

/**
 * Persists the last moment local data was known to match the server (the `serverTime` of the last
 * successful sync, epoch millis). Null means "never synced". Powers a "last synced X ago" indicator.
 */
interface LastServerContactStore {
    val lastContactAt: StateFlow<Long?>

    /** Records [serverTimeMillis] (the sync response's cursor) as the last server contact. */
    fun record(serverTimeMillis: Long)

    /** Drops the timestamp (e.g. on sign-out) so a new session starts without a stale value. */
    fun clear()
}
