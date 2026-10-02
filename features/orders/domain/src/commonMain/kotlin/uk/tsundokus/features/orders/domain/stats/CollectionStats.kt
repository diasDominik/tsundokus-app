package uk.tsundokus.features.orders.domain.stats

import uk.tsundokus.core.domain.preferences.AppCurrency
import uk.tsundokus.features.orders.domain.dates.daysBetween
import uk.tsundokus.features.orders.domain.dates.isoFromEpochMillis
import uk.tsundokus.features.orders.domain.dates.parseIsoDate
import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.ReadState
import uk.tsundokus.features.orders.domain.series.toSeries

/** The stretch of time spend, top lists, delivery and store reliability are worked out over. */
enum class StatsPeriod {
    /** This month and the eleven before it, a bar per month. */
    LAST_12_MONTHS,

    /** January to this month, a bar per month. */
    THIS_YEAR,

    /** Everything, a bar per year. */
    ALL_TIME,
}

/**
 * The collection in numbers, worked out from the orders alone. Cancelled orders are left out
 * everywhere: they were never bought. [pile] and [collection] are what the user has now, whatever
 * the period; the rest only counts orders placed in it.
 */
data class CollectionStats(
    val pile: PileStats,
    val collection: CollectionCounts,
    /** One entry per currency: the user's own first, then the most used. Amounts are never converted. */
    val spend: List<CurrencySpend>,
    val topStores: List<RankedName>,
    val topPublishers: List<RankedName>,
    val delivery: DeliveryStats,
    /** Most often delayed first; only stores with [MIN_ORDERS_FOR_RELIABILITY] orders or more. */
    val storeReliability: List<StoreReliability>,
) {
    /** Nothing ordered yet (or only cancelled orders): there is nothing to show. */
    val isEmpty: Boolean get() = collection.ordered == 0
}

/** Arrived and not read yet: the tsundoku. */
data class PileStats(
    val unread: Int,
    /** Of [unread], the books being read right now. */
    val reading: Int,
    /** How long the longest-waiting unread book (not one being read) has been here; null if none has a date. */
    val oldestWaitingDays: Int?,
)

data class CollectionCounts(
    /** Every order that is not cancelled, arrived or not. */
    val ordered: Int,
    val owned: Int,
    /** Owned and read. */
    val read: Int,
    /** Series with at least one volume owned. */
    val series: Int,
)

/** What was spent in one currency over the period. */
data class CurrencySpend(
    val currency: AppCurrency,
    val total: Double,
    val volumes: Int,
    /** The mean price of the volumes that have one; null when none does. */
    val averagePrice: Double?,
    /** Committed to orders still on the way — ordered, shipped or delayed — whenever they were placed. */
    val onTheWay: Double,
    val buckets: List<SpendBucket>,
)

/** One bar of the spend chart: a month, or a whole year when [month] is null. */
data class SpendBucket(
    val year: Int,
    val month: Int?,
    val amount: Double,
)

data class RankedName(
    val name: String,
    val count: Int,
)

/** Medians, so one parcel lost for three months doesn't make every delivery look slow. */
data class DeliveryStats(
    /** Days from dispatch to arrival. */
    val medianShippingDays: Int?,
    val shippingSample: Int,
    /**
     * Days from ordering to arrival, counted from the release date for a pre-order: waiting for a book
     * to come out is not the store being slow.
     */
    val medianWaitDays: Int?,
    val waitSample: Int,
)

data class StoreReliability(
    val store: String,
    val orders: Int,
    val delayed: Int,
    /** How much later than promised a delayed order was expected, in days; null when none says. */
    val medianSlipDays: Int?,
) {
    val delayRate: Double get() = delayed.toDouble() / orders
}

const val MIN_ORDERS_FOR_RELIABILITY = 3
private const val TOP_COUNT = 5

/**
 * Works the stats out as of [today] (ISO `yyyy-MM-dd`). [ownCurrency] is the user's chosen currency,
 * whose spend is listed first.
 */
fun List<Order>.toCollectionStats(
    today: String,
    period: StatsPeriod,
    ownCurrency: AppCurrency,
): CollectionStats {
    val active = filter { it.status != OrderStatus.CANCELLED }
    val owned = active.filter { it.status == OrderStatus.RECEIVED }
    val todayDate = parseIsoDate(today) ?: error("today must be an ISO date: $today")
    val buckets = bucketsFor(period, todayDate.year, todayDate.month, active)
    val inPeriod = active.filter { order -> buckets.any { it.contains(order.spendDate) } }
    return CollectionStats(
        pile = owned.toPile(today),
        collection =
            CollectionCounts(
                ordered = active.size,
                owned = owned.size,
                read = owned.count { it.readState == ReadState.READ },
                series =
                    active.toSeries().count { series ->
                        series.orders.any { it.status == OrderStatus.RECEIVED }
                    },
            ),
        spend = spendByCurrency(active, inPeriod, buckets, ownCurrency),
        topStores = inPeriod.topNames(Order::store),
        topPublishers = inPeriod.topNames(Order::publisher),
        delivery = inPeriod.toDelivery(),
        storeReliability = inPeriod.toStoreReliability(),
    )
}

/** When an order counts as spent: the day it was placed, else the day it was added to the app. */
private val Order.spendDate: String
    get() = orderDate.takeIf { parseIsoDate(it) != null } ?: isoFromEpochMillis(createdAt)

private fun List<Order>.toPile(today: String): PileStats {
    val unread = filter { it.readState != ReadState.READ }
    return PileStats(
        unread = unread.size,
        reading = unread.count { it.readState == ReadState.READING },
        oldestWaitingDays =
            unread
                .filter { it.readState == ReadState.WANT }
                .mapNotNull { daysBetween(it.receivedDate, today) }
                .maxOrNull()
                ?.coerceAtLeast(0),
    )
}

/** A bar's time span: a month, or a year when [month] is null. */
private data class BucketSpan(
    val year: Int,
    val month: Int?,
) {
    fun contains(iso: String): Boolean {
        val date = parseIsoDate(iso) ?: return false
        return date.year == year && (month == null || date.month == month)
    }
}

private fun bucketsFor(
    period: StatsPeriod,
    year: Int,
    month: Int,
    orders: List<Order>,
): List<BucketSpan> =
    when (period) {
        StatsPeriod.LAST_12_MONTHS -> {
            (11 downTo 0).map { back ->
                val index = year * 12 + (month - 1) - back
                BucketSpan(index / 12, index % 12 + 1)
            }
        }

        StatsPeriod.THIS_YEAR -> {
            (1..month).map { BucketSpan(year, it) }
        }

        StatsPeriod.ALL_TIME -> {
            val years = orders.mapNotNull { parseIsoDate(it.spendDate)?.year }
            val first = years.minOrNull() ?: year
            val last = maxOf(years.maxOrNull() ?: year, year)
            (first..last).map { BucketSpan(it, null) }
        }
    }

private fun spendByCurrency(
    active: List<Order>,
    inPeriod: List<Order>,
    buckets: List<BucketSpan>,
    ownCurrency: AppCurrency,
): List<CurrencySpend> {
    val onTheWay = active.filter { it.status != OrderStatus.RECEIVED }
    return (inPeriod + onTheWay)
        .map(Order::currency)
        .distinctBy(AppCurrency::code)
        .map { currency ->
            val spent = inPeriod.filter { it.currency.code == currency.code }
            val priced = spent.filter { it.price > 0.0 }
            CurrencySpend(
                currency = currency,
                total = spent.sumOf(Order::price),
                volumes = spent.size,
                averagePrice = priced.takeIf { it.isNotEmpty() }?.let { it.sumOf(Order::price) / it.size },
                onTheWay = onTheWay.filter { it.currency.code == currency.code }.sumOf(Order::price),
                buckets =
                    buckets.map { span ->
                        SpendBucket(
                            year = span.year,
                            month = span.month,
                            amount = spent.filter { span.contains(it.spendDate) }.sumOf(Order::price),
                        )
                    },
            )
        }.sortedWith(
            compareByDescending<CurrencySpend> { it.currency.code == ownCurrency.code }
                .thenByDescending(CurrencySpend::volumes)
                .thenByDescending(CurrencySpend::onTheWay),
        )
}

/**
 * The names used most, counted case- and space-insensitively and shown in the spelling used most,
 * so one "amazon" among the "Amazon"s doesn't make a second store.
 */
private fun List<Order>.topNames(name: (Order) -> String): List<RankedName> =
    map { name(it).trim() }
        .filter(String::isNotEmpty)
        .groupBy(::nameKey)
        .map { (_, spellings) -> RankedName(spellings.mostCommon(), spellings.size) }
        .sortedWith(compareByDescending(RankedName::count).thenBy(String.CASE_INSENSITIVE_ORDER, RankedName::name))
        .take(TOP_COUNT)

private fun List<Order>.toDelivery(): DeliveryStats {
    val received = filter { it.status == OrderStatus.RECEIVED }
    val shipping = received.mapNotNull { daysBetween(it.shipDate, it.receivedDate)?.takeIf { days -> days >= 0 } }
    val waits = received.mapNotNull { it.waitDays() }
    return DeliveryStats(
        medianShippingDays = shipping.median(),
        shippingSample = shipping.size,
        medianWaitDays = waits.median(),
        waitSample = waits.size,
    )
}

/** Days from ordering — or from release, for a pre-order — to arrival. */
private fun Order.waitDays(): Int? {
    if (parseIsoDate(orderDate) == null) return null
    val start = if (parseIsoDate(releaseDate) != null) maxOf(orderDate, releaseDate) else orderDate
    return daysBetween(start, receivedDate)?.takeIf { it >= 0 }
}

/**
 * Stores judged on the orders whose outcome is known: arrived, or already reported delayed. An order
 * that arrived after a delay still counts as delayed — reporting one keeps its delayed-to date.
 */
private fun List<Order>.toStoreReliability(): List<StoreReliability> =
    filter { it.store.isNotBlank() && (it.status == OrderStatus.RECEIVED || it.wasDelayed) }
        .groupBy { nameKey(it.store) }
        .values
        .filter { it.size >= MIN_ORDERS_FOR_RELIABILITY }
        .map { orders ->
            val delayed = orders.filter(Order::wasDelayed)
            StoreReliability(
                store = orders.map { it.store.trim() }.mostCommon(),
                orders = orders.size,
                delayed = delayed.size,
                medianSlipDays = delayed.mapNotNull { it.slipDays() }.median(),
            )
        }.sortedWith(
            compareByDescending(StoreReliability::delayRate)
                .thenByDescending(StoreReliability::orders)
                .thenBy(String.CASE_INSENSITIVE_ORDER, StoreReliability::store),
        ).take(TOP_COUNT)

private val Order.wasDelayed: Boolean
    get() = status == OrderStatus.DELAYED || delayedTo.isNotBlank()

/** How much later the delayed-to date is than the one first promised: the ETA, else the release date. */
private fun Order.slipDays(): Int? {
    val promised = eta.ifBlank { releaseDate }
    return daysBetween(promised, delayedTo)?.takeIf { it > 0 }
}

private fun nameKey(name: String): String = name.lowercase().filterNot(Char::isWhitespace)

private fun List<String>.mostCommon(): String =
    groupingBy { it }
        .eachCount()
        .maxBy { it.value }
        .key

/** The middle value, the two middle ones' mean rounded up when there is an even number; null if empty. */
private fun List<Int>.median(): Int? {
    if (isEmpty()) return null
    val sorted = sorted()
    val middle = size / 2
    return if (size % 2 == 1) sorted[middle] else (sorted[middle - 1] + sorted[middle] + 1) / 2
}
