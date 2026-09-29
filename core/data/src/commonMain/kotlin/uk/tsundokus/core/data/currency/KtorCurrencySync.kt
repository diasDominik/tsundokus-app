package uk.tsundokus.core.data.currency

import eu.anifantakis.lib.ksafe.KSafe
import eu.anifantakis.lib.ksafe.KSafeWriteMode
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.di.APPLICATION_SCOPE
import uk.tsundokus.core.data.networking.get
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.core.domain.preferences.CurrencyCatalog
import uk.tsundokus.core.domain.preferences.CurrencySync
import uk.tsundokus.core.domain.util.onSuccess

@Serializable
data class CurrencyDto(
    val code: String,
    val symbol: String,
    val decimals: Int,
    val name: String,
    val symbolAfterAmount: Boolean = false,
    val regions: List<String> = emptyList(),
)

/**
 * The currency list comes from the server, so a new currency needs no app release. The last list
 * fetched is kept, so it is there offline and from the first frame of the next launch.
 */
@Single(binds = [CurrencySync::class])
class KtorCurrencySync(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    @Named("prefs") private val prefs: KSafe,
    private val json: Json,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) : CurrencySync {
    private var job: Job? = null

    override fun start() {
        if (job != null) return
        job =
            appScope.launch {
                cached()?.let(::apply)
                // Once per sign-in: the list changes only with a server deploy.
                sessionStorage.authState
                    .map { it != null }
                    .distinctUntilChanged()
                    .filter { it }
                    .collect { refresh() }
            }
    }

    private suspend fun refresh() {
        httpClient.get<List<CurrencyDto>>(route = "/api/currencies").onSuccess { currencies ->
            apply(currencies)
            prefs.put(KEY, json.encodeToString(currencies), KSafeWriteMode.Plain)
        }
    }

    private suspend fun cached(): List<CurrencyDto>? {
        val stored = prefs.get(KEY, "")
        if (stored.isBlank()) return null
        // A list cached by an older app version that no longer decodes is simply fetched again.
        return try {
            json.decodeFromString<List<CurrencyDto>>(stored)
        } catch (_: SerializationException) {
            null
        }
    }

    private fun apply(currencies: List<CurrencyDto>) {
        CurrencyCatalog.update(
            currencies =
                currencies.map {
                    AppCurrency(
                        it.code,
                        it.symbol,
                        it.decimals,
                        it.name,
                        it.symbolAfterAmount,
                    )
                },
            regions = currencies.associate { it.code to it.regions },
        )
    }

    private companion object {
        const val KEY = "currencies"
    }
}
