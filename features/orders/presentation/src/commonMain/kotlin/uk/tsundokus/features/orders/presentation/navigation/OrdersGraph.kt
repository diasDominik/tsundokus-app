package uk.tsundokus.features.orders.presentation.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass
import uk.tsundokus.core.presentation.util.SnackbarController
import uk.tsundokus.features.orders.presentation.addeditorder.AddEditOrderArgs
import uk.tsundokus.features.orders.presentation.addeditorder.AddEditOrderRoot
import uk.tsundokus.features.orders.presentation.orderdetail.OrderDetailRoot
import uk.tsundokus.features.orders.presentation.orderslist.OrdersListRoot
import uk.tsundokus.features.orders.presentation.readinglist.ReadingListRoot
import uk.tsundokus.features.orders.presentation.reportdelay.ReportDelayRoot
import uk.tsundokus.features.orders.presentation.scantoreceive.ScanToReceiveRoot
import uk.tsundokus.features.orders.presentation.seriesdetail.SeriesDetailRoot
import uk.tsundokus.features.orders.presentation.serieslist.SeriesListRoot
import uk.tsundokus.features.orders.presentation.stats.StatsRoot

val ordersSerializersModule =
    SerializersModule {
        polymorphic(NavKey::class) {
            subclass(Orders::class)
            subclass(ReadingList::class)
            subclass(SeriesList::class)
            subclass(SeriesDetail::class)
            subclass(Stats::class)
            subclass(OrderDetail::class)
            subclass(AddOrder::class)
            subclass(EditOrder::class)
            subclass(ReportDelay::class)
            subclass(ScanToReceive::class)
        }
    }

/**
 * Wires every orders route. Forward navigation is expressed through callbacks (so the host owns
 * back-stack semantics); [backStack] is provided for routes that need direct manipulation.
 */
fun EntryProviderScope<NavKey>.ordersGraph(
    backStack: NavBackStack<NavKey>,
    onOpenOrder: (String) -> Unit,
    onAddOrder: (AddOrder) -> Unit,
    onScanToReceive: () -> Unit,
    onOpenSeries: (String) -> Unit,
    onEditOrder: (String) -> Unit,
    onReportDelay: (String) -> Unit,
    onOpenReading: () -> Unit,
    onBack: () -> Unit,
    snackbar: SnackbarController,
) {
    entry<Orders> {
        OrdersListRoot(
            onOpenOrder = onOpenOrder,
            onScanToReceive = onScanToReceive,
            onEditOrder = onEditOrder,
            onReportDelay = onReportDelay,
            snackbar = snackbar,
        )
    }

    entry<ReadingList> {
        ReadingListRoot(
            onOpenOrder = onOpenOrder,
            snackbar = snackbar,
        )
    }

    entry<SeriesList> {
        SeriesListRoot(
            onOpenSeries = onOpenSeries,
            onOpenOrder = onOpenOrder,
            onOrderVolume = { prefill -> onAddOrder(AddOrder(prefill = prefill)) },
            snackbar = snackbar,
        )
    }

    entry<Stats> {
        StatsRoot(
            onOpenReading = onOpenReading,
            snackbar = snackbar,
        )
    }

    entry<SeriesDetail> { route ->
        SeriesDetailRoot(
            seriesKey = route.seriesKey,
            onOpenOrder = onOpenOrder,
            onOrderVolume = { prefill -> onAddOrder(AddOrder(prefill = prefill)) },
        )
    }

    entry<OrderDetail> { route ->
        OrderDetailRoot(
            orderId = route.orderId,
            snackbar = snackbar,
            onEdit = { onEditOrder(route.orderId) },
            onReportDelay = { onReportDelay(route.orderId) },
            onBack = onBack,
        )
    }

    entry<AddOrder> { route ->
        AddEditOrderRoot(
            navKey = route,
            args = AddEditOrderArgs(initialIsbn = route.isbn, prefill = route.prefill),
            onSaved = onBack,
            onClose = onBack,
            snackbar = snackbar,
        )
    }

    entry<EditOrder> { route ->
        AddEditOrderRoot(
            navKey = route,
            args = AddEditOrderArgs(orderId = route.orderId),
            onSaved = onBack,
            onClose = onBack,
            snackbar = snackbar,
        )
    }

    entry<ScanToReceive> {
        ScanToReceiveRoot(
            onOpenOrder = onOpenOrder,
            onAddOrder = { isbn -> onAddOrder(AddOrder(isbn = isbn)) },
            snackbar = snackbar,
        )
    }

    entry<ReportDelay> { route ->
        ReportDelayRoot(
            orderId = route.orderId,
            onSaved = onBack,
            snackbar = snackbar,
        )
    }
}
