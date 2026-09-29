package uk.tsundokus.features.orders.domain.book

import uk.tsundokus.core.domain.util.DataError
import uk.tsundokus.core.domain.util.Result

/** Looks books up by ISBN, through the server's shared cache of the book databases. */
interface BookRepository {
    /** [isbn] as ISBN-13 digits. Success(null) when no database knows the book. */
    suspend fun lookup(isbn: String): Result<BookInfo?, DataError.Remote>
}
