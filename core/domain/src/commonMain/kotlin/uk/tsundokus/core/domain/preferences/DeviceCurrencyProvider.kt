package uk.tsundokus.core.domain.preferences

/** The currency the device's region settings imply — the default for someone who has not picked one. */
interface DeviceCurrencyProvider {
    /** Null when the platform cannot tell. */
    fun currentCurrency(): AppCurrency?
}
