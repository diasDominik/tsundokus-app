package uk.tsundokus.features.authentication.testing

import kotlinx.coroutines.delay
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.authentication.domain.AuthService

open class FakeAuthService(
    var registerResult: Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN),
    var loginResult: Result<AuthInfo, DataError.Remote> = Result.Failure(DataError.Remote.UNKNOWN),
    var loginDelayMillis: Long = 0L,
    var forgotPasswordResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var resetPasswordResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var verifyEmailResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var resendVerificationEmailResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
    var logoutResult: EmptyResult<DataError.Remote> = Result.Success(Unit),
) : AuthService {
    val logoutCalls: MutableList<String> = mutableListOf()
    val verifyEmailCalls: MutableList<String> = mutableListOf()
    val resendVerificationEmailCalls: MutableList<String> = mutableListOf()

    var registerCalls: Int = 0
        private set

    var loginCalls: Int = 0
        private set

    var forgotPasswordCalls: Int = 0
        private set

    var resetPasswordCalls: Int = 0
        private set

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        registerCalls += 1
        return registerResult
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        loginCalls += 1
        if (loginDelayMillis > 0L) {
            delay(loginDelayMillis)
        }
        return loginResult
    }

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> {
        forgotPasswordCalls += 1
        return forgotPasswordResult
    }

    override suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote> {
        resetPasswordCalls += 1
        return resetPasswordResult
    }

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
        verifyEmailCalls += token
        return verifyEmailResult
    }

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> {
        resendVerificationEmailCalls += email
        return resendVerificationEmailResult
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        logoutCalls += refreshToken
        return logoutResult
    }
}
