package uk.tsundokus.core.domain.legal

/**
 * Public legal pages. Deliberately not derived from the configured backend URL: the app can be
 * pointed at a different server, but the privacy policy must always resolve to the published
 * document regardless of which backend the build talks to.
 *
 * Kept here rather than in either feature so the sign-up notice and the settings row cannot drift
 * apart — both link to the same policy, and a stale copy in one of them is a legal problem, not a
 * cosmetic one.
 */
object LegalUrls {
    const val PRIVACY_POLICY = "https://github.com/diasDominik/tsundokus-app/blob/main/PRIVACY.md"
}
