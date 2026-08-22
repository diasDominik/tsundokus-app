package uk.tsundokus.core.domain.auth

/**
 * Ends the session without losing track of whose it was.
 *
 * The only sanctioned way to clear an *involuntarily* ended session (a rejected refresh token): it
 * records a [StaleSession] *before* clearing [SessionStorage], which is what lets the app keep the
 * local order cache and ask the same account back in with its email pre-filled. Deliberate sign-out
 * and account deletion do **not** go through here — those wipe the cache instead.
 */
interface SessionInvalidator {
    fun invalidate()
}
