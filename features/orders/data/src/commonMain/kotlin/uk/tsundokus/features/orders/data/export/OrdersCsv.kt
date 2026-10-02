package uk.tsundokus.features.orders.data.export

import uk.tsundokus.features.orders.domain.models.Order
import uk.tsundokus.features.orders.domain.models.formatAmount

/**
 * The CSV header. English and fixed, not translated: a spreadsheet or script reading the file relies
 * on the names, whatever language the app is in.
 */
internal val CSV_COLUMNS =
    listOf(
        "id",
        "title",
        "volume",
        "author",
        "publisher",
        "isbn",
        "store",
        "price",
        "currency",
        "status",
        "read_state",
        "order_date",
        "release_date",
        "ship_date",
        "eta",
        "received_date",
        "delayed_to",
        "added_at",
    )

/**
 * [orders] as RFC 4180 CSV: comma-separated, CRLF line ends, fields quoted when they need to be. It
 * starts with a byte order mark, without which Excel reads UTF-8 as a legacy code page and mangles
 * every title not in plain English.
 */
internal fun ordersToCsv(orders: List<Order>): String =
    buildString {
        append(BYTE_ORDER_MARK)
        appendCsvRow(CSV_COLUMNS)
        orders.forEach { appendCsvRow(it.csvCells()) }
    }

private const val BYTE_ORDER_MARK = '﻿'

private fun Order.csvCells(): List<String> =
    listOf(
        id,
        guardFormula(title),
        guardFormula(volume),
        guardFormula(author),
        guardFormula(publisher),
        isbn,
        guardFormula(store),
        formatAmount(price, currency.decimals),
        currency.code,
        status.name,
        readState.name,
        orderDate,
        releaseDate,
        shipDate,
        eta,
        receivedDate,
        delayedTo,
        addedAtIso(createdAt),
    )

private fun StringBuilder.appendCsvRow(cells: List<String>) {
    cells.joinTo(this, separator = ",", transform = ::escapeCsvCell)
    append("\r\n")
}

/** Quoted, with its quotes doubled, when it holds a comma, a quote or a line break; as is otherwise. */
internal fun escapeCsvCell(cell: String): String =
    if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
        "\"" + cell.replace("\"", "\"\"") + "\""
    } else {
        cell
    }

/**
 * A spreadsheet runs a cell starting with one of these as a formula, so a title like `=HYPERLINK(…)`
 * typed into an order would run when the file is opened. A leading `'` makes it plain text; an import
 * would take it off again.
 */
private val FORMULA_STARTS = setOf('=', '+', '-', '@', '\t', '\r')

internal fun guardFormula(text: String): String = if (text.firstOrNull() in FORMULA_STARTS) "'$text" else text
