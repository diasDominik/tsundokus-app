package uk.tsundokus.features.authentication.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.clearAuthTokens
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.dto.AuthInfoSerializable
import uk.tsundokus.core.data.dto.UserSerializable
import uk.tsundokus.core.data.dto.requests.RefreshRequest
import uk.tsundokus.core.data.mappers.toDomain
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.post
import uk.tsundokus.core.domain.auth.AuthInfo
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.authentication.data.dto.requests.EmailRequest
import uk.tsundokus.features.authentication.data.dto.requests.LoginRequest
import uk.tsundokus.features.authentication.data.dto.requests.RegisterRequest
import uk.tsundokus.features.authentication.data.dto.requests.ResetPasswordRequest
import uk.tsundokus.features.authentication.domain.AuthService

@Single(binds = [AuthService::class])
class KtorAuthService(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
) : AuthService {
    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): EmptyResult<DataError.Remote> {
        // Registration does NOT log the user in. The server creates the account (unverified) and
        // emails a verification link; the user must verify, then sign in.
        return httpClient.post<RegisterRequest, Unit>(
            route = "/api/auth/register",
            body =
                RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                ),
        )
    }

    override suspend fun login(
        email: String,
        password: String,
    ): Result<AuthInfo, DataError.Remote> {
        return httpClient
            .post<LoginRequest, AuthInfoSerializable>(
                route = "/api/auth/login",
                body =
                    LoginRequest(
                        email = email,
                        password = password,
                    ),
            ).map { authInfoSerializable ->
                authInfoSerializable.toDomain()
            }.onSuccess { authInfo ->
                sessionStorage.set(authInfo)
            }
    }

    override suspend fun forgotPassword(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post<EmailRequest, Unit>(
            route = "/api/auth/forgot-password",
            body = EmailRequest(email),
        )
    }

    override suspend fun resetPassword(
        newPassword: String,
        token: String,
    ): EmptyResult<DataError.Remote> {
        return httpClient.post(
            route = "/api/auth/reset-password",
            body =
                ResetPasswordRequest(
                    newPassword = newPassword,
                    token = token,
                ),
        )
    }

    override suspend fun verifyEmail(token: String): EmptyResult<DataError.Remote> {
        return httpClient
            .get<Unit>(
                route = "/api/auth/verify",
                queryParams = mapOf("token" to token),
            ).onSuccess {
                // Refresh the cached session user's verification flag if we have an active session.
                val current = sessionStorage.get()
                if (current != null) {
                    httpClient
                        .get<UserSerializable>(route = "/api/auth/me")
                        .onSuccess { me ->
                            sessionStorage.set(current.copy(user = me.toDomain()))
                        }
                }
            }
    }

    override suspend fun resendVerificationEmail(email: String): EmptyResult<DataError.Remote> {
        return httpClient.post<EmailRequest, Unit>(
            route = "/api/auth/resend-verification",
            body = EmailRequest(email),
        )
    }

    override suspend fun logout(refreshToken: String): EmptyResult<DataError.Remote> {
        return httpClient
            .post<RefreshRequest, Unit>(
                route = "/api/auth/logout",
                body = RefreshRequest(refreshToken = refreshToken),
            ).onSuccess {
                httpClient.clearAuthTokens()
            }
    }
}
