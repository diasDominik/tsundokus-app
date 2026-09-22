package uk.tsundokus.features.settings.data.passkeys

import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.networking.delete
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.post
import uk.tsundokus.core.domain.auth.PasskeyCeremony
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.EmptyResult
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.features.settings.data.dto.PasskeyCeremonySerializable
import uk.tsundokus.features.settings.data.dto.PasskeyFinishRequest
import uk.tsundokus.features.settings.data.dto.PasskeySerializable
import uk.tsundokus.features.settings.domain.passkeys.Passkey
import uk.tsundokus.features.settings.domain.passkeys.PasskeyService

@Single(binds = [PasskeyService::class])
class KtorPasskeyService(
    private val httpClient: HttpClient,
) : PasskeyService {
    override suspend fun passkeys(): Result<List<Passkey>, DataError.Remote> =
        httpClient
            .get<List<PasskeySerializable>>(route = "/api/auth/passkeys")
            .map { passkeys -> passkeys.map(PasskeySerializable::toDomain) }

    override suspend fun beginEnrolment(): Result<PasskeyCeremony, DataError.Remote> =
        httpClient
            .post<Unit, PasskeyCeremonySerializable>(
                route = "/api/auth/passkeys/register/begin",
                body = Unit,
            ).map { ceremony -> PasskeyCeremony(ceremony.ceremonyId, ceremony.optionsJson) }

    override suspend fun finishEnrolment(
        ceremonyId: String,
        responseJson: String,
        label: String?,
    ): Result<Passkey, DataError.Remote> =
        httpClient
            .post<PasskeyFinishRequest, PasskeySerializable>(
                route = "/api/auth/passkeys/register/finish",
                body =
                    PasskeyFinishRequest(
                        ceremonyId = ceremonyId,
                        responseJson = responseJson,
                        label = label,
                    ),
            ).map(PasskeySerializable::toDomain)

    override suspend fun delete(credentialId: String): EmptyResult<DataError.Remote> =
        httpClient.delete<Unit>(route = "/api/auth/passkeys/$credentialId")
}

private fun PasskeySerializable.toDomain(): Passkey =
    Passkey(
        credentialId = credentialId,
        label = label,
        createdAt = createdAt,
        lastUsedAt = lastUsedAt,
    )
