package uk.tsundokus.features.orders.domain.stats

import uk.tsundokus.features.orders.domain.dates.daysBetween
import uk.tsundokus.features.orders.domain.dates.isoPlusDays
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus

/** Where an [ArrivalEstimate]'s shipping time comes from, the most specific first. */
enum class EstimateBasis {
    /** The median of this store's own deliveries: the store decides how it ships. */
    STORE,

    /** Too few deliveries from this store: the median of every store's. */
    ALL_STORES,

    /** Too few deliveries at all: [DEFAULT_SHIPPING_DAYS], what letter post usually takes. */
    DEFAULT,
}

/**
 * When a shipped order with no ETA of its own should arrive, judged from the user's past deliveries.
 * Only ever shown as an estimate, and never saved: the user's own date always wins.
 */
data class ArrivalEstimate(
    /** ISO `yyyy-MM-dd`. */
    val date: String,
    val basis: EstimateBasis,
    /** How many deliveries the median was taken over; 0 for [EstimateBasis.DEFAULT]. */
    val sample: Int,
)

/** Fewer deliveries than this and a median says more about luck than about the store. */
const val MIN_DELIVERIES_FOR_ESTIMATE = 3

/** Only the latest deliveries count, so a store that changed how it ships is judged on how it ships now. */
const val MAX_DELIVERIES_FOR_ESTIMATE = 20

/** A Warensendung usually takes about four days. */
const val DEFAULT_SHIPPING_DAYS = 4

/**
 * How long the user's deliveries took, dispatch to door, per store and overall. Built once from all
 * the orders, then asked about each order still on the way.
 */
class ShippingTimes internal constructor(
    private val byStore: Map<String, List<Int>>,
    private val overall: List<Int>,
) {
    /**
     * The estimate for [order], or null when it needs none: it isn't shipped, has its own ETA, or
     * has no ship date to count from.
     */
    fun estimateFor(order: Order): ArrivalEstimate? {
        if (order.status != OrderStatus.SHIPPED || order.eta.isNotBlank()) return null
        return expectedArrival(order)
    }

    /**
     * When [order] would be expected from its ship date and store, whatever its status or ETA; null
     * without a ship date.
     */
    fun expectedArrival(order: Order): ArrivalEstimate? {
        val fromStore = byStore[nameKey(order.store)].orEmpty()
        val (days, basis, sample) =
            when {
                order.store.isNotBlank() && fromStore.size >= MIN_DELIVERIES_FOR_ESTIMATE -> {
                    Triple(fromStore.median(), EstimateBasis.STORE, fromStore.size)
                }

                overall.size >= MIN_DELIVERIES_FOR_ESTIMATE -> {
                    Triple(overall.median(), EstimateBasis.ALL_STORES, overall.size)
                }

                else -> {
                    Triple(DEFAULT_SHIPPING_DAYS, EstimateBasis.DEFAULT, 0)
                }
            }
        val date = isoPlusDays(order.shipDate, days ?: DEFAULT_SHIPPING_DAYS) ?: return null
        return ArrivalEstimate(date, basis, sample)
    }

    companion object {
        /** Nothing to go on: every estimate is the default. */
        val None = ShippingTimes(emptyMap(), emptyList())
    }
}

/** The shipping times of the orders in [this] that arrived with both dates set. */
fun List<Order>.shippingTimes(): ShippingTimes {
    val deliveries =
        filter { it.status == OrderStatus.RECEIVED }
            .mapNotNull { order ->
                daysBetween(order.shipDate, order.receivedDate)
                    ?.takeIf { it >= 0 }
                    ?.let { days -> Delivery(nameKey(order.store), order.receivedDate, days) }
            }.sortedByDescending(Delivery::receivedDate)
    return ShippingTimes(
        byStore =
            deliveries
                .filter { it.storeKey.isNotEmpty() }
                .groupBy(Delivery::storeKey)
                .mapValues { (_, fromStore) -> fromStore.latestDays() },
        overall = deliveries.latestDays(),
    )
}

private data class Delivery(
    val storeKey: String,
    val receivedDate: String,
    val days: Int,
)

/** Already sorted latest first. */
private fun List<Delivery>.latestDays(): List<Int> = take(MAX_DELIVERIES_FOR_ESTIMATE).map(Delivery::days)
