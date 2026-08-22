package uk.tsundokus.core.domain.sync

/**
 * Wipes every trace of the signed-in account's data from the device (the local order cache).
 *
 * The single wipe path, so "what does signing out actually delete" has one answer. Lives in
 * `core.domain` because both the settings screens and the sign-in flow need it and neither
 * `:features:settings:*` nor `:features:authentication:*` may depend on `:features:orders:*`.
 */
interface LocalDataResetter {
    suspend fun resetLocalData()
}
