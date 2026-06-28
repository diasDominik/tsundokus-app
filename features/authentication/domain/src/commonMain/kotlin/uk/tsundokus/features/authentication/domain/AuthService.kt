package uk.tsundokus.features.authentication.domain

import uk.tsundokus.core.domain.auth.AuthInfo
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

    suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote>

    suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote>

    suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote>

    suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote>

    suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote>
}
