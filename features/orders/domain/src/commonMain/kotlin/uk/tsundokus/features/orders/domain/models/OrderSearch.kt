package uk.tsundokus.features.orders.domain.models

/**
 * Whether [query] finds this order: its title, author or publisher contain it, or its ISBN does,
 * typed with hyphens or not. One rule for every list that searches, so one habit works everywhere.
 */
fun Order.matchesQuery(query: String): Boolean {
    if (query.isBlank()) return true
    val needle = query.trim()
    // Stored without hyphens, so they are dropped from the query before comparing.
    val isbnNeedle = needle.filterNot { it == '-' || it == ' ' }
    return title.contains(needle, ignoreCase = true) ||
        author.contains(needle, ignoreCase = true) ||
        publisher.contains(needle, ignoreCase = true) ||
        (isbnNeedle.isNotEmpty() && isbn.contains(isbnNeedle, ignoreCase = true))
}
