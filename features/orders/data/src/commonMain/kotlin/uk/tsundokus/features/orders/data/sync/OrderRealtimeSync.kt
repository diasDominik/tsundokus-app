package uk.tsundokus.features.orders.data.sync

import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.http.encodeURLParameter
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.AppBuildInfo
import uk.tsundokus.core.data.di.APPLICATION_SCOPE
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.core.domain.sync.LastServerContactStore
import uk.tsundokus.features.orders.data.dto.OrderChangeDto
import uk.tsundokus.features.orders.data.mappers.toDomain
import uk.tsundokus.features.orders.data.mappers.toEntity
import uk.tsundokus.features.orders.database.TsundokuDatabase
import uk.tsundokus.features.orders.domain.order.OrderRepository

/**
 * Realtime order sync. While signed in, holds a WebSocket to `/ws/orders` and applies pushed changes
 * to the local cache the moment they happen on another device. Complements — does not replace — the
 * outbox/delta pull: on every (re)connect it triggers a delta pull to close any gap opened while the
 * socket was down, and it never advances the sync cursor from a socket frame (only the delta pull
 * does), so a missed frame is always reconciled.
 */
@Single
class OrderRealtimeSync(
    private val httpClient: HttpClient,
    private val sessionStorage: SessionStorage,
    private val database: TsundokuDatabase,
    private val orderRepository: OrderRepository,
    private val lastServerContactStore: LastServerContactStore,
    private val json: Json,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private var job: Job? = null

    /** Idempotent: starts the connect-while-signed-in loop once. */
    fun start() {
        if (job != null) return
        job =
            appScope.launch {
                // collectLatest cancels the live connection the instant the session goes away.
                sessionStorage.authState
                    .map { it != null }
                    .distinctUntilChanged()
                    .collectLatest { signedIn -> if (signedIn) connectLoop() }
            }
    }

    private suspend fun connectLoop() {
        while (currentCoroutineContext().isActive) {
            val token = sessionStorage.get()?.accessToken ?: return
            try {
                httpClient.webSocket(urlString = webSocketUrl(token)) {
                    // Catch up on anything missed while the socket was down.
                    orderRepository.fetchOrders()
                    for (frame in incoming) {
                        if (frame is Frame.Text) applyChange(frame.readText())
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // Connection dropped or refused; fall through to the backoff and retry.
            }
            delay(RECONNECT_DELAY_MS)
        }
    }

    private suspend fun applyChange(text: String) {
        val change = runCatching { json.decodeFromString<OrderChangeDto>(text) }.getOrNull() ?: return
        // Never overwrite a row that still has an unsynced local write; its outbox op is the truth.
        val pendingIds =
            database.pendingOrderOpDao
                .getAll()
                .map { it.orderId }
                .toSet()
        when (change.type) {
            "UPSERT" -> {
                change.order?.let { dto ->
                    if (dto.id !in pendingIds) {
                        database.orderDao.upsert(dto.toDomain().toEntity(pendingSync = false))
                    }
                }
            }

            "DELETE" -> {
                change.id?.let { id ->
                    if (id !in pendingIds) database.orderDao.deleteById(id)
                }
            }
        }
        lastServerContactStore.record(change.serverTime)
    }

    private fun webSocketUrl(token: String): String {
        val base = AppBuildInfo.baseUrl
        val wsBase =
            when {
                base.startsWith("https://") -> "wss://" + base.removePrefix("https://")
                base.startsWith("http://") -> "ws://" + base.removePrefix("http://")
                else -> base
            }.trimEnd('/')
        val query = "token=${token.encodeURLParameter()}&apiKey=${AppBuildInfo.apiKey.encodeURLParameter()}"
        return "$wsBase/ws/orders?$query"
    }

    private companion object {
        const val RECONNECT_DELAY_MS = 5_000L
    }
}
