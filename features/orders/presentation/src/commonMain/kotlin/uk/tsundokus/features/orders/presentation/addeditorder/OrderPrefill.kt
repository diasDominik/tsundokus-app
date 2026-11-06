package uk.tsundokus.features.orders.presentation.addeditorder

import kotlinx.serialization.Serializable

/**
 * What a new order starts filled in with — the next volume of a series, say. Part of the `AddOrder`
 * route, so it survives process death; [currencyCode] is an ISO 4217 code and [price] is as the form
 * shows it, in that currency.
 */
@Serializable
data class OrderPrefill(
    val title: String = "",
    val author: String = "",
    val publisher: String = "",
    val store: String = "",
    val volume: String = "",
    val currencyCode: String = "",
    val price: String = "",
)
