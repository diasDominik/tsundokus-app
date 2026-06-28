package uk.tsundokus.features.settings.data.settings

import io.ktor.client.HttpClient
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.data.networking.patch
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.ThemeMode
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result
import uk.tsundokus.core.domain.util.map
import uk.tsundokus.features.settings.data.dto.SettingsDto
import uk.tsundokus.features.settings.data.dto.UpdateSettingsRequest
import uk.tsundokus.features.settings.data.mappers.toDomain
import uk.tsundokus.features.settings.domain.models.AppSettings
import uk.tsundokus.features.settings.domain.settings.SettingsService

@Single(binds = [SettingsService::class])
class KtorSettingsService(
    private val httpClient: HttpClient,
) : SettingsService {
    override suspend fun getSettings(): Result<AppSettings, DataError.Remote> =
        httpClient
            .get<SettingsDto>(route = "/api/settings")
            .map { it.toDomain() }

    override suspend fun updateSettings(
        theme: ThemeMode?,
        currency: AppCurrency?,
    ): Result<AppSettings, DataError.Remote> =
        httpClient
            .patch<UpdateSettingsRequest, SettingsDto>(
                route = "/api/settings",
                body =
                    UpdateSettingsRequest(
                        theme = theme?.name,
                        currency = currency?.name,
                    ),
            ).map { it.toDomain() }
}
