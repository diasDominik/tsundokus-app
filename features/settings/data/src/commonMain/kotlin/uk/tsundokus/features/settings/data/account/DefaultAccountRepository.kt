package uk.tsundokus.features.settings.data.account

import org.koin.core.annotation.Single
import uk.tsundokus.core.domain.auth.User
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.features.settings.domain.account.AccountRepository
import uk.tsundokus.features.settings.domain.account.AccountService

@Single(binds = [AccountRepository::class])
class DefaultAccountRepository(
    private val accountService: AccountService,
) : AccountRepository {
    override suspend fun getMe(): Result<User, DataError.Remote> = accountService.getMe()

    override suspend fun updateProfile(name: String): Result<User, DataError.Remote> =
        accountService.updateProfile(name)

    override suspend fun changeEmail(
        newEmail: String,
        currentPassword: String,
    ): Result<User, DataError.Remote> = accountService.changeEmail(newEmail, currentPassword)

    override suspend fun changePassword(
        currentPassword: String,
        newPassword: String,
    ): EmptyResult<DataError.Remote> = accountService.changePassword(currentPassword, newPassword)

    override suspend fun deleteAccount(): EmptyResult<DataError.Remote> = accountService.deleteAccount()
}
