package uk.tsundokus.core.data.networking

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.statement.request
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import uk.tsundokus.core.data.BuildKonfig
import uk.tsundokus.core.data.dto.AuthInfoSerializable
import uk.tsundokus.core.data.dto.requests.RefreshRequest
import uk.tsundokus.core.data.mappers.toDomain
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.logging.TsundokuLogger
import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.onFailure
import uk.tsundokus.core.domain.util.onSuccess

private const val TAG = "HttpClientFactory"

class HttpClientFactory(
    private val tsundokuLogger: TsundokuLogger,
    private val sessionStorage: SessionStorage,
    private val json: Json,
) {
    fun create(engine: HttpClientEngine): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(json = json)
            }
            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }
            install(Logging) {
                logger =
                    object : Logger {
                        override fun log(message: String) {
                            tsundokuLogger.debug(TAG, message)
                        }
                    }
                level = if (BuildKonfig.IS_DEBUG) LogLevel.ALL else LogLevel.INFO
            }
            defaultRequest {
                header("x-api-key", BuildKonfig.API_KEY)
                contentType(ContentType.Application.Json)
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        sessionStorage.get()?.let {
                            BearerTokens(
                                accessToken = it.accessToken,
                                refreshToken = it.refreshToken,
                            )
                        }
                    }
                    refreshTokens {
                        if (response.request.url.encodedPath
                                .contains("api/auth/")
                        ) {
                            return@refreshTokens null
                        }
                        val localAuthInfo = sessionStorage.get()
                        if (localAuthInfo?.refreshToken.isNullOrBlank()) {
                            return@refreshTokens null
                        }

                        var bearerTokens: BearerTokens? = null
                        client
                            .post<RefreshRequest, AuthInfoSerializable>(
                                route = "/api/auth/refresh",
                                body =
                                    RefreshRequest(
                                        refreshToken = localAuthInfo.refreshToken,
                                    ),
                                builder = {
                                    markAsRefreshTokenRequest()
                                },
                            ).onSuccess { newAuthInfo ->
                                sessionStorage.set(newAuthInfo.toDomain())
                                bearerTokens =
                                    BearerTokens(
                                        accessToken = newAuthInfo.accessToken,
                                        refreshToken = newAuthInfo.refreshToken,
                                    )
                            }.onFailure {
                                if (it.isAuthRejection()) {
                                    sessionStorage.set(null)
                                }
                            }

                        bearerTokens
                    }
                }
            }
        }
    }

    private fun DataError.Remote.isAuthRejection(): Boolean =
        this == DataError.Remote.UNAUTHORIZED ||
            this == DataError.Remote.FORBIDDEN
}
