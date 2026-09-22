package uk.tsundokus.features.settings.domain.passkeys

import uk.tsundokus.core.domain.auth.PasskeyCeremony
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result

/**
 * Managing the passkeys on the signed-in account. Enrolling one needs an existing session — a
 * passkey is another way into an account, never a way to open one — so this lives with the account
 * settings rather than with sign-in.
 *
 * Implemented in :features:settings:data by KtorPasskeyService.
 */
interface PasskeyService {
    suspend fun passkeys(): Result<List<Passkey>, DataError.Remote>

    /** Asks the server for an enrolment challenge for this account. */
    suspend fun beginEnrolment(): Result<PasskeyCeremony, DataError.Remote>

    /** Hands back what the authenticator signed, naming the new passkey. */
    suspend fun finishEnrolment(
        ceremonyId: String,
        responseJson: String,
        label: String?,
    ): Result<Passkey, DataError.Remote>

    suspend fun delete(credentialId: String): EmptyResult<DataError.Remote>
}
