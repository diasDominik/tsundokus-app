package uk.tsundokus.features.orders.presentation.navigation

import androidx.compose.material3.SnackbarHostState
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import uk.tsundokus.features.orders.presentation.addeditorder.AddEditOrderRoot
import uk.tsundokus.features.orders.presentation.orderdetail.OrderDetailRoot
import uk.tsundokus.features.orders.presentation.orderslist.OrdersListRoot
import uk.tsundokus.features.orders.presentation.readinglist.ReadingListRoot
import uk.tsundokus.features.orders.presentation.reportdelay.ReportDelayRoot

val ordersSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Orders::class)
            subclass(ReadingList::class)
            subclass(OrderDetail::class)
            subclass(AddOrder::class)
            subclass(EditOrder::class)
            subclass(ReportDelay::class)
        }
    }

/**
 * Wires every orders route. Forward navigation is expressed through callbacks (so the host owns
 * back-stack semantics); [backStack] is provided for routes that need direct manipulation.
 */
fun EntryProviderScope<NavKey>.ordersGraph(
    backStack: NavBackStack<NavKey>,
    onOpenOrder: (String) -> Unit,
    onAddOrder: () -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    entry<Orders> {
        OrdersListRoot(
            onOpenOrder = onOpenOrder,
            onEditOrder = onEditOrder,
            onReportDelay = onReportDelay,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<ReadingList> {
        ReadingListRoot(
            onOpenOrder = onOpenOrder,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<OrderDetail> { route ->
        OrderDetailRoot(
            orderId = route.orderId,
            snackbarHostState = snackbarHostState,
            onEdit = { onEditOrder(route.orderId) },
            onReportDelay = { onReportDelay(route.orderId) },
            onBack = onBack,
        )
    }

    entry<AddOrder> {
        AddEditOrderRoot(
            orderId = null,
            onSaved = onBack,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<EditOrder> { route ->
        AddEditOrderRoot(
            orderId = route.orderId,
            onSaved = onBack,
            snackbarHostState = snackbarHostState,
        )
    }

    entry<ReportDelay> { route ->
        ReportDelayRoot(
            orderId = route.orderId,
            onSaved = onBack,
            snackbarHostState = snackbarHostState,
        )
    }
}
