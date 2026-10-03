package uk.tsundokus.features.orders.domain.models

import kotlin.math.abs
import kotlin.math.round

/** [value] with exactly [decimals] fraction digits, rounded; no grouping, "." as the separator. */
fun formatAmount(
    value: Double,
    decimals: Int,
): String {
    var scale = 1L
    repeat(decimals) { scale *= 10 }
    val minor = round(value * scale).toLong()
    val sign = if (minor < 0) "-" else ""
    val magnitude = abs(minor)
    val whole = magnitude / scale
    if (decimals == 0) return "$sign$whole"
    val fraction = (magnitude % scale).toString().padStart(decimals, '0')
    return "$sign$whole.$fraction"
}
