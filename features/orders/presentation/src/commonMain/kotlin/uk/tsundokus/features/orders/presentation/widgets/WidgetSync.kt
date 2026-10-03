package uk.tsundokus.features.orders.presentation.widgets

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.annotation.Named
import org.koin.core.annotation.Single
import uk.tsundokus.core.data.di.APPLICATION_SCOPE
import uk.tsundokus.core.domain.auth.SessionStorage
import uk.tsundokus.features.orders.domain.dates.todayIso
import uk.tsundokus.features.orders.domain.order.OrderRepository

/**
 * Keeps the home-screen widgets in step with the cached orders while the app runs. Signed out, the
 * widgets are handed the signed-out snapshot, so order titles don't stay on the home screen.
 */
@Single
class WidgetSync(
    private val orderRepository: OrderRepository,
    private val sessionStorage: SessionStorage,
    private val publisher: WidgetPublisher,
    @Named(APPLICATION_SCOPE) private val appScope: CoroutineScope,
) {
    private var job: Job? = null

    /** Idempotent: starts following the orders once, for the life of the app. */
    fun start() {
        if (job != null) return
        job =
            appScope.launch {
                combine(
                    orderRepository.getOrders(),
                    sessionStorage.authState.map { it != null },
                ) { orders, signedIn ->
                    if (signedIn) orders.toWidgetSnapshot(todayIso()) else WidgetSnapshot.SignedOut
                }
                    // A sync that changes nothing a widget shows redraws nothing.
                    .distinctUntilChanged()
                    .collectLatest { publisher.publish(it) }
            }
    }
}
