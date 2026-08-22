package uk.tsundokus.core.domain.sync

/**
 * Persists the delta-sync watermark (the `serverTime` from the last successful `/api/sync/orders`,
 * epoch millis). Echoed back as `since` on the next sync so the server returns only later changes.
 * Null means "no successful sync yet" → the next sync pulls a full snapshot.
 */
interface SyncCursorStore {
    fun get(): Long?

    fun set(cursor: Long)

    /** Drops the watermark (e.g. on sign-out) so the next sync starts from a full snapshot. */
    fun clear()
}
