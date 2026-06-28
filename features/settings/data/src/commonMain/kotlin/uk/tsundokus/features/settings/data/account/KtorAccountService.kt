package uk.tsundokus.features.settings.data.account

import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.dto.UserSerializable
import uk.tsundokus.core.data.mappers.toDomain
import uk.tsundokus.core.data.networking.delete
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.patch
import uk.tsundokus.core.data.networking.post
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.asEmptyResult
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.core.domain.util.onSuccess
import uk.tsundokus.features.settings.data.dto.ChangeEmailRequest
import uk.tsundokus.features.settings.data.dto.ChangePasswordRequest
import uk.tsundokus.features.settings.data.dto.UpdateProfileRequest
import uk.tsundokus.features.settings.domain.account.AccountService

@Single(binds = [AccountService::class])
class KtorAccountService(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
) : AccountService {
    override suspend fun getMe(): Result<User, DataError.Remote> =
        httpClient
            .get<UserSerializable>(route = "/api/auth/me")
            .map { it.toDomain() }
            .onSuccess { user -> syncSession(user) }

    override suspend fun updateProfile(name: String): Result<User, DataError.Remote> =
        httpClient
            .patch<UpdateProfileRequest, UserSerializable>(
                route = "/api/auth/profile",
                body = UpdateProfileRequest(name = name),
            ).map { it.toDomain() }
            .onSuccess { user -> syncSession(user) }

    override suspend fun changeEmail(
        newEmail: String,
        currentPassword: String,
    ): Result<User, DataError.Remote> =
        httpClient
            .post<ChangeEmailRequest, UserSerializable>(
                route = "/api/auth/change-email",
                body =
                    ChangeEmailRequest(
                        newEmail = newEmail,
                        currentPassword = currentPassword,
                    ),
            ).map { it.toDomain() }
            .onSuccess { user -> syncSession(user) }

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> =
        httpClient.post<ChangePasswordRequest, Unit>(
            route = "/api/auth/change-password",
            body =
                ChangePasswordRequest(
                    currentPassword = currentPassword,
                    newPassword = newPassword,
                ),
        )

    override suspend fun deleteAccount(): EmptyResult<DataError.Remote> =
        httpClient
            .delete<Unit>(route = "/api/auth/account")
            .asEmptyResult()

    /** Keep the cached session in sync so the new profile/email shows everywhere immediately. */
    private fun syncSession(user: User) {
        sessionStorage.get()?.let { current ->
            sessionStorage.set(current.copy(user = user))
        }
    }
}
