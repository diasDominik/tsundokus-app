package uk.tsundokus.core.domain.preferences

/** Keeps the app's currency list ([CurrencyCatalog]) in step with the server's. */
interface CurrencySync {
    /** Idempotent: loads the cached list, then refreshes it from the server whenever someone signs in. */
    fun start()
}
