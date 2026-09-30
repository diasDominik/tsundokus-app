package uk.tsundokus.features.orders.presentation.navigation

/** Path of the link that opens an order — `tsundokus://tsundokus.uk/orders/{orderId}`. */
const val ORDER_LINK_PATH = "/orders"

/** The link a reminder opens: the app's custom scheme, so it never leaves the app. */
fun orderDeepLink(orderId: String): String = "tsundokus://tsundokus.uk$ORDER_LINK_PATH/$orderId"
