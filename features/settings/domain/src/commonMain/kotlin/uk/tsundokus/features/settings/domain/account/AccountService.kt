package uk.tsundokus.features.settings.domain.account

import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result

/** Remote API for the signed-in account. Implemented in :features:settings:data by KtorAccountService. */
interface AccountService {
    suspend fun getMe(): Result<User, DataError.Remote>

    suspend fun updateProfile(name: String): Result<User, DataError.Remote>

    suspend fun changeEmail(
        newEmail: String,
        currentPassword: String,
    ): Result<User, DataError.Remote>

    suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote>

    suspend fun deleteAccount(): EmptyResult<DataError.Remote>
}
