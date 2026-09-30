package uk.tsundokus.features.orders.domain.series

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.OrderStatus
import uk.tsundokus.features.orders.domain.models.matchesQuery

enum class VolumeState {
    /** At least one order for this volume has arrived. */
    RECEIVED,

    /** Ordered, shipped or delayed, and none received yet. */
    ON_THE_WAY,
}

/** One volume of a series, with every order behind it (a volume ordered twice keeps both). */
data class SeriesVolume(
    val number: Int,
    val state: VolumeState,
    val orders: List<Order>,
) {
    /** The order a tap on this volume opens: the newest. */
    val latestOrder: Order get() = orders.maxBy(Order::createdAt)
}

/**
 * A series as the user's orders describe it. Series are not stored anywhere: orders whose titles
 * match once case, punctuation and a volume number are ignored ([key]) are one series, so a wrong
 * grouping is fixed by editing a title.
 */
data class Series(
    val key: String,
    val title: String,
    val orders: List<Order>,
    val volumes: List<SeriesVolume>,
    /** Unnumbered orders in a numbered series: special editions, artbooks. */
    val extras: List<Order>,
) {
    /** The newest order: where author, publisher, shop and currency for the next volume come from. */
    val latest: Order get() = orders.maxBy(Order::createdAt)

    val author: String get() = latest.author
    val publisher: String get() = latest.publisher

    /** No numbered volume at all: a one-shot, shown as a single book. */
    val isOneShot: Boolean get() = volumes.isEmpty()

    /**
     * The series at a glance, as the most pressing state among its orders: delayed before shipped before
     * ordered, and received once everything has arrived — the order the Orders list groups by.
     */
    val status: OrderStatus
        get() = OrderStatus.groupOrder.first { status -> orders.any { it.status == status } }

    val receivedCount: Int get() = volumes.count { it.state == VolumeState.RECEIVED }
    val onTheWayCount: Int get() = volumes.count { it.state == VolumeState.ON_THE_WAY }

    /**
     * Volumes between the lowest and highest the user has that they do not have. Not from 1: someone
     * who started a series at volume 5 has not missed 1 to 4.
     */
    val missing: List<Int>
        get() {
            if (volumes.isEmpty()) return emptyList()
            val numbers = volumes.map(SeriesVolume::number).toSet()
            // Sorted by number, so the ends of the list are the ends of the range.
            return (volumes.first().number..volumes.last().number).filterNot { it in numbers }
        }

    val nextVolume: Int? get() = volumes.maxOfOrNull(SeriesVolume::number)?.plus(1)

    /** When anything in this series last happened — the sort key for "recent". */
    val lastActivity: Long get() = orders.maxOf(Order::createdAt)

    /** The ISBN whose cover stands for the series: the highest volume that has one, else any order. */
    val coverIsbn: String?
        get() =
            volumes
                .sortedByDescending(SeriesVolume::number)
                .firstNotNullOfOrNull { volume -> volume.orders.firstOrNull { it.hasCover }?.isbn }
                ?: orders.firstOrNull { it.hasCover }?.isbn

    /**
     * The volume field for [nextVolume], in the user's own format: the newest numbered order's
     * "Vol. 12" becomes "Vol. 13", a bare "12" becomes "13".
     */
    val nextVolumeLabel: String?
        get() {
            val next = nextVolume ?: return null
            val field = volumes.last().latestOrder.volume
            val number = VolumeNumber.NUMBER.findAll(field).lastOrNull() ?: return next.toString()
            return field.replaceRange(number.range, next.toString())
        }

    fun matchesQuery(query: String): Boolean =
        title.contains(query.trim(), ignoreCase = true) || orders.any { it.matchesQuery(query) }
}

/** Case, spacing and punctuation don't make a different series; letters of any script count. */
fun seriesKey(title: String): String = title.lowercase().filter(Char::isLetterOrDigit)

/** Groups orders into series. Cancelled orders are left out: they are not part of the collection. */
fun List<Order>.toSeries(): List<Series> =
    filter { it.status != OrderStatus.CANCELLED }
        .map { order -> order to VolumeNumber.parse(order) }
        .groupBy { (_, volume) -> seriesKey(volume.title) }
        .filterKeys(String::isNotEmpty)
        .map { (key, entries) ->
            Series(
                key = key,
                // The spelling used most, so one typo doesn't rename the series.
                title =
                    entries
                        .groupingBy { (_, volume) -> volume.title }
                        .eachCount()
                        .maxBy { it.value }
                        .key,
                orders = entries.map { (order, _) -> order },
                volumes =
                    entries
                        .mapNotNull { (order, volume) -> volume.number?.let { it to order } }
                        .groupBy({ (number, _) -> number }, { (_, order) -> order })
                        .map { (number, orders) -> SeriesVolume(number, volumeState(orders), orders) }
                        .sortedBy(SeriesVolume::number),
                extras = entries.filter { (_, volume) -> volume.number == null }.map { (order, _) -> order },
            )
        }

private fun volumeState(orders: List<Order>): VolumeState =
    if (orders.any { it.status == OrderStatus.RECEIVED }) VolumeState.RECEIVED else VolumeState.ON_THE_WAY
