package uk.tsundokus.features.authentication.domain

import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.PasskeyCeremony
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result

interface AuthService {
    suspend fun register(
        name: String,
        email: String,
        password: String,
    ): EmptyResult<DataError.Remote>

    suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote>

    /**
     * Asks the server for a sign-in challenge. Deliberately takes no email: the request is not
     * scoped to an account, so it cannot be used to find out which accounts exist, and the
     * authenticator offers whichever passkey it holds for this site.
     */
    suspend fun beginPasskeyLogin(): Result<PasskeyCeremony, DataError.Remote>

    /** Hands back what the authenticator signed. Succeeds into a session like [login] does. */
    suspend fun finishPasskeyLogin(
        ceremonyId: String,
        responseJson: String,
    ): Result<AuthInfo, DataError.Remote>

    suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote>

    suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote>

    suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote>

    suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote>

    suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote>
}
