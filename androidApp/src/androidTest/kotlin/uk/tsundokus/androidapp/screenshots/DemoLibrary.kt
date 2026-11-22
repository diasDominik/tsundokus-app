package uk.tsundokus.androidapp.screenshots

import org.json.JSONObject
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * The made-up collection the store screenshots show. Series, authors, publishers and shops are
 * invented, so the screenshots carry no third-party titles or cover art.
 *
 * Dates are relative to [today], so every run shows the same picture: something delayed, parcels
 * on the way, pre-orders, a reading pile and a year of deliveries for the stats.
 */
internal class DemoLibrary(
    private val today: LocalDate = LocalDate.now(),
) {
    data class Series(
        val title: String,
        val author: String,
        val publisher: String,
        val color: Int,
    )

    data class Order(
        val id: String,
        val series: Series,
        val volume: Int,
        val store: String,
        val price: Double,
        val status: String,
        val readState: String,
        val orderDate: LocalDate,
        val releaseDate: LocalDate?,
        val shipDate: LocalDate?,
        val eta: LocalDate?,
        val receivedDate: LocalDate?,
        val delayedTo: LocalDate?,
    ) {
        val isbn: String = isbn13(id.hashCode().toLong() and 0xFFFFFFF)
    }

    private val moonlitBakery = Series("Moonlit Bakery", "Aoi Takahara", "Hikari Press", 0xFFC8553D.toInt())
    private val ninthStarCourier =
        Series("Ninth Star Courier", "Ren Kisaragi", "Starfall Comics", 0xFF3D5A80.toInt())
    private val paperLantern = Series("Paper Lantern Detective", "Mio Hasegawa", "Kumo Books", 0xFF8E3B46.toInt())
    private val glassGarden = Series("Glass Garden", "Sora Ishikawa", "Hikari Press", 0xFF4F7C6B.toInt())
    private val saltAndThunder = Series("Salt & Thunder", "Kenta Morimoto", "Starfall Comics", 0xFF22808A.toInt())
    private val littleFox = Series("Little Fox Library", "Yuki Arai", "Kumo Books", 0xFFD98A2B.toInt())

    private val comicPlanet = "Comic Planet"
    private val pageTurner = "Page Turner"
    private val otakuCorner = "Otaku Corner"

    val orders: List<Order> =
        listOf(
            received(moonlitBakery, 1, pageTurner, 7.0, orderedAgo = 320, receivedAgo = 314, readState = READ),
            received(moonlitBakery, 2, pageTurner, 7.0, orderedAgo = 260, receivedAgo = 255, readState = READ),
            received(moonlitBakery, 3, comicPlanet, 7.0, orderedAgo = 200, receivedAgo = 192, readState = READ),
            received(moonlitBakery, 4, comicPlanet, 7.0, orderedAgo = 140, receivedAgo = 133, readState = READING),
            received(moonlitBakery, 5, pageTurner, 7.5, orderedAgo = 75, receivedAgo = 70, readState = WANT),
            shipped(moonlitBakery, 6, pageTurner, 7.5, orderedAgo = 6, shippedAgo = 1, etaIn = 1),
            ordered(moonlitBakery, 7, comicPlanet, 7.5, orderedAgo = 12, releaseIn = 24),
            received(ninthStarCourier, 1, otakuCorner, 9.0, orderedAgo = 300, receivedAgo = 290, readState = READ),
            received(
                ninthStarCourier,
                2,
                otakuCorner,
                9.0,
                orderedAgo = 230,
                receivedAgo = 218,
                readState = READ,
                daysLate = 4,
            ),
            received(
                ninthStarCourier,
                3,
                otakuCorner,
                9.0,
                orderedAgo = 110,
                receivedAgo = 96,
                readState = WANT,
                daysLate = 5,
            ),
            delayed(ninthStarCourier, 4, otakuCorner, 9.0, orderedAgo = 40, etaAgo = 3, delayedToIn = 9),
            received(paperLantern, 1, comicPlanet, 8.0, orderedAgo = 280, receivedAgo = 275, readState = READ),
            received(paperLantern, 2, comicPlanet, 8.0, orderedAgo = 180, receivedAgo = 176, readState = READ),
            received(paperLantern, 4, pageTurner, 8.0, orderedAgo = 50, receivedAgo = 45, readState = WANT),
            ordered(paperLantern, 5, comicPlanet, 8.0, orderedAgo = 20, releaseIn = 45),
            received(glassGarden, 1, pageTurner, 12.0, orderedAgo = 95, receivedAgo = 88, readState = WANT),
            shipped(glassGarden, 2, comicPlanet, 12.0, orderedAgo = 14, shippedAgo = 2, etaIn = 2),
            received(saltAndThunder, 1, otakuCorner, 6.5, orderedAgo = 160, receivedAgo = 150, readState = READ),
            received(saltAndThunder, 2, otakuCorner, 6.5, orderedAgo = 60, receivedAgo = 52, readState = WANT),
            received(saltAndThunder, 3, pageTurner, 6.5, orderedAgo = 25, receivedAgo = 19, readState = WANT),
            ordered(littleFox, 1, pageTurner, 10.0, orderedAgo = 3, releaseIn = 60),
        )

    /** The order the detail screenshot opens: a delay has a timeline worth showing. */
    val featured: Order = orders.single { it.status == DELAYED }

    fun order(isbn: String): Order? = orders.firstOrNull { it.isbn == isbn }

    fun syncResponse(): JSONObject =
        JSONObject()
            .put("serverTime", System.currentTimeMillis())
            .put("changed", orders.map(::toJson).toJsonArray())
            .put("deleted", emptyList<String>().toJsonArray())

    private fun toJson(order: Order): JSONObject =
        JSONObject()
            .put("id", order.id)
            .put("title", order.series.title)
            .put("author", order.series.author)
            .put("publisher", order.series.publisher)
            .put("volume", order.volume.toString())
            .put("store", order.store)
            .put("price", order.price)
            .put("currency", "EUR")
            .put("status", order.status)
            .put("readState", order.readState)
            .put("orderDate", order.orderDate.toString())
            .put("releaseDate", order.releaseDate?.toString() ?: JSONObject.NULL)
            .put("shipDate", order.shipDate?.toString() ?: JSONObject.NULL)
            .put("eta", order.eta?.toString() ?: JSONObject.NULL)
            .put("receivedDate", order.receivedDate?.toString() ?: JSONObject.NULL)
            .put("delayedTo", order.delayedTo?.toString() ?: JSONObject.NULL)
            .put("isbn", order.isbn)
            .put("hasCover", true)
            .put(
                "createdAt",
                order.orderDate
                    .atStartOfDay()
                    .toInstant(ZoneOffset.UTC)
                    .toEpochMilli(),
            )

    private fun received(
        series: Series,
        volume: Int,
        store: String,
        price: Double,
        orderedAgo: Long,
        receivedAgo: Long,
        readState: String,
        daysLate: Long = 0,
    ): Order {
        val shipped = today.minusDays(receivedAgo + 2)
        return Order(
            id = id(series, volume),
            series = series,
            volume = volume,
            store = store,
            price = price,
            status = RECEIVED,
            readState = readState,
            orderDate = today.minusDays(orderedAgo),
            releaseDate = shipped,
            shipDate = shipped,
            eta = today.minusDays(receivedAgo + daysLate),
            receivedDate = today.minusDays(receivedAgo),
            delayedTo = null,
        )
    }

    private fun shipped(
        series: Series,
        volume: Int,
        store: String,
        price: Double,
        orderedAgo: Long,
        shippedAgo: Long,
        etaIn: Long,
    ): Order =
        Order(
            id = id(series, volume),
            series = series,
            volume = volume,
            store = store,
            price = price,
            status = SHIPPED,
            readState = WANT,
            orderDate = today.minusDays(orderedAgo),
            releaseDate = today.minusDays(shippedAgo),
            shipDate = today.minusDays(shippedAgo),
            eta = today.plusDays(etaIn),
            receivedDate = null,
            delayedTo = null,
        )

    private fun ordered(
        series: Series,
        volume: Int,
        store: String,
        price: Double,
        orderedAgo: Long,
        releaseIn: Long,
    ): Order =
        Order(
            id = id(series, volume),
            series = series,
            volume = volume,
            store = store,
            price = price,
            status = ORDERED,
            readState = WANT,
            orderDate = today.minusDays(orderedAgo),
            releaseDate = today.plusDays(releaseIn),
            shipDate = null,
            eta = today.plusDays(releaseIn + 2),
            receivedDate = null,
            delayedTo = null,
        )

    private fun delayed(
        series: Series,
        volume: Int,
        store: String,
        price: Double,
        orderedAgo: Long,
        etaAgo: Long,
        delayedToIn: Long,
    ): Order =
        Order(
            id = id(series, volume),
            series = series,
            volume = volume,
            store = store,
            price = price,
            status = DELAYED,
            readState = WANT,
            orderDate = today.minusDays(orderedAgo),
            releaseDate = today.minusDays(etaAgo + 2),
            shipDate = null,
            eta = today.minusDays(etaAgo),
            receivedDate = null,
            delayedTo = today.plusDays(delayedToIn),
        )

    private fun id(
        series: Series,
        volume: Int,
    ): String = "demo-" + series.title.lowercase().replace(Regex("[^a-z]+"), "-") + "-$volume"

    private companion object {
        const val ORDERED = "ORDERED"
        const val SHIPPED = "SHIPPED"
        const val DELAYED = "DELAYED"
        const val RECEIVED = "RECEIVED"
        const val WANT = "WANT"
        const val READING = "READING"
        const val READ = "READ"

        /** A valid ISBN-13 in the 979-8 range, from [seed]. */
        fun isbn13(seed: Long): String {
            val body = "9798" + (seed % 100_000_000).toString().padStart(8, '0')
            val sum = body.mapIndexed { index, digit -> (digit - '0') * if (index % 2 == 0) 1 else 3 }.sum()
            return body + (10 - sum % 10) % 10
        }

        fun List<Any>.toJsonArray() = org.json.JSONArray(this)
    }
}
