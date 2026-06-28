package uk.tsundokus.features.settings.domain.account

import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result

/**
 * Thin repository wrapper over [AccountService]. Account data is single-source (remote), so this
 * simply forwards calls; it exists for call sites that prefer a repository-shaped abstraction.
 */
interface AccountRepository {
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
